import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Assembler {

    private static final Parser AInstructionParser = new AInstructionParser();
    private static final Parser CInstructionParser = new CInstructionParser();
    private static final Map<String, String> SYMBOL_TABLE = new HashMap<>();

    private static final AtomicInteger CURRENT_LINE = new AtomicInteger(0);

    static {
        SYMBOL_TABLE.put("R0", "0");
        SYMBOL_TABLE.put("R1", "1");
        SYMBOL_TABLE.put("R2", "2");
        SYMBOL_TABLE.put("R3", "3");
        SYMBOL_TABLE.put("R4", "4");
        SYMBOL_TABLE.put("R5", "5");
        SYMBOL_TABLE.put("R6", "6");
        SYMBOL_TABLE.put("R7", "7");
        SYMBOL_TABLE.put("R8", "8");
        SYMBOL_TABLE.put("R9", "9");
        SYMBOL_TABLE.put("R10", "10");
        SYMBOL_TABLE.put("R11", "11");
        SYMBOL_TABLE.put("R12", "12");
        SYMBOL_TABLE.put("R13", "13");
        SYMBOL_TABLE.put("R14", "14");
        SYMBOL_TABLE.put("R15", "15");
        SYMBOL_TABLE.put("SP", "0");
        SYMBOL_TABLE.put("LCL", "1");
        SYMBOL_TABLE.put("ARG", "2");
        SYMBOL_TABLE.put("THIS", "3");
        SYMBOL_TABLE.put("THAT", "4");
        SYMBOL_TABLE.put("SCREEN", "16384");
        SYMBOL_TABLE.put("KBD", "24576");
    }

    private static String hexToBinary(String value, int length){
        int data = Integer.parseInt(value);
        StringBuilder binaryCode = new StringBuilder(Integer.toBinaryString(data));
        if (binaryCode.length() > length){
            throw new RuntimeException(String.format("the length %d should be shorter than value %s after convert %s", length, binaryCode.toString(), value));
        }
        int len = binaryCode.length();
        if (len < length){
            for (int i = 0; i < length - len; i++){
                binaryCode.insert(0, "0");
            }
        }
        return binaryCode.toString();
    }

    private interface Parser{
        String parseLine(String line);
    }

    private static class SymbolParser implements Parser{

        @Override
        public String parseLine(String line) {
            return null;
        }
    }

    private static class AInstructionParser implements Parser{

        private static final String A_INSTRUCTION_PREFIX = "0";
        private static final AtomicInteger CURRENT_VARIABLE_NUMBER = new AtomicInteger(0);
        private static final int VARIABLE_START_INDEX = 16;
        public String parseLine(String line) {
            String symbol = line.replace("@", "");
            try {
                Integer.parseInt(symbol);
            }catch (NumberFormatException e){
                if (SYMBOL_TABLE.get(symbol) == null){
                    int index = VARIABLE_START_INDEX + CURRENT_VARIABLE_NUMBER.get();
                    SYMBOL_TABLE.put(symbol, String.valueOf(index));
                    symbol = String.valueOf(index);
                    CURRENT_VARIABLE_NUMBER.incrementAndGet();
                }else {
                    symbol = SYMBOL_TABLE.get(symbol);
                }
            }
            CURRENT_LINE.incrementAndGet();
            return A_INSTRUCTION_PREFIX + hexToBinary(symbol, 15);
        }
    }

    private static class CInstructionParser implements Parser{

        private static final HashMap<String, String > JMP_TABLE = new HashMap<>();
        private static final HashMap<String, String > DEST_TABLE = new HashMap<>();
        private static final HashMap<String, String > COMP_TABLE = new HashMap<>();
        private static final String C_INSTRUCTION_PREFIX = "111";

        static {
            DEST_TABLE.put("null", "000");
            DEST_TABLE.put("", "000");
            DEST_TABLE.put("M", "001");
            DEST_TABLE.put("D", "010");
            DEST_TABLE.put("MD", "011");
            DEST_TABLE.put("A", "100");
            DEST_TABLE.put("AM", "101");
            DEST_TABLE.put("AD", "110");
            DEST_TABLE.put("AMD", "111");

            JMP_TABLE.put("null", "000");
            JMP_TABLE.put("", "000");
            JMP_TABLE.put("JGT", "001");
            JMP_TABLE.put("JEQ", "010");
            JMP_TABLE.put("JGE", "011");
            JMP_TABLE.put("JLT", "100");
            JMP_TABLE.put("JNE", "101");
            JMP_TABLE.put("JLE", "110");
            JMP_TABLE.put("JMP", "111");

            COMP_TABLE.put("0", "0101010");
            COMP_TABLE.put("1", "0111111");
            COMP_TABLE.put("-1","0111010");
            COMP_TABLE.put("D", "0001100");
            COMP_TABLE.put("A", "0110000");
            COMP_TABLE.put("!D", "0001101");
            COMP_TABLE.put("!A", "0110001");
            COMP_TABLE.put("-D", "0001111");
            COMP_TABLE.put("-A", "0110011");
            COMP_TABLE.put("D+1", "0011111");
            COMP_TABLE.put("A+1", "0110111");
            COMP_TABLE.put("D-1", "0001110");
            COMP_TABLE.put("A-1", "0110010");
            COMP_TABLE.put("D+A", "0000010");
            COMP_TABLE.put("D-A", "0010011");
            COMP_TABLE.put("A-D", "0000111");
            COMP_TABLE.put("D&A", "0000000");
            COMP_TABLE.put("D|A", "0010101");
            COMP_TABLE.put("M", "1110000");
            COMP_TABLE.put("!M", "1110001");
            COMP_TABLE.put("-M", "1110011");
            COMP_TABLE.put("M+1", "1110111");
            COMP_TABLE.put("M-1", "1110010");
            COMP_TABLE.put("D+M", "1000010");
            COMP_TABLE.put("D-M", "1010011");
            COMP_TABLE.put("M-D", "1000111");
            COMP_TABLE.put("D&M", "1000000");
            COMP_TABLE.put("D|M", "1010101");
        }

        public String parseLine(String line) {
            StringBuilder sb = new StringBuilder();
            sb.append(C_INSTRUCTION_PREFIX);
            String jumpCode = JMP_TABLE.get("");
            String destCode = DEST_TABLE.get("");
            if (line.contains("=")){
                String[] data = line.split("=");
                if (data.length != 2){
                    throw new RuntimeException(String.format("%s format is error", line));
                }
                if (DEST_TABLE.get(data[0]) == null){
                    throw new RuntimeException(String.format("%s dest is error", line));
                }
                destCode = DEST_TABLE.get(data[0]);
            }

            if (line.contains(";")){
                String[] data = line.split(";");
                if (data.length != 2){
                    throw new RuntimeException(String.format("%s format is error", line));
                }
                if (JMP_TABLE.get(data[1]) == null){
                    throw new RuntimeException(String.format("%s dest is error", line));
                }
                jumpCode = JMP_TABLE.get(data[1]);
            }

            String comp = line.replaceAll(".*=", "").replaceAll(";.*", "");
            String compCode = COMP_TABLE.get(comp);
            if (compCode == null)
                throw new RuntimeException();
            CURRENT_LINE.incrementAndGet();
            return C_INSTRUCTION_PREFIX + compCode + destCode + jumpCode;
        }
    }

    private static Parser getParser(String line){
        if (line.startsWith("@")){
            return AInstructionParser;
        }else if (line.startsWith("(")){
            return new SymbolParser();
        }else {
            return CInstructionParser;
        }
    }

    private static String parseToBinaryCode(String line){
        return getParser(line).parseLine(line);
    }

    private static void preHandleSymbol(List<String> data){
        int count = 0;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).startsWith("(")){
                String symbol = data.get(i).replace("(", "").replace(")", "");
                SYMBOL_TABLE.put(symbol, String.valueOf(i - count));
                count++;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=".split("1=").length);
        if (args == null || args.length < 2 || args[0] == null || args[1] == null){
            System.err.println("输入参数错误，至少包括两个输入参数，第一个为汇编语言路径，第二个为输出文件路径");
            return;
        }
        try {
            List<String> data = Files.readAllLines(Paths.get(new File(args[0]).toURI()))
                    .stream()
                    .map(it-> it.replaceAll("//.*","" ))
                    .map(it->it.replaceAll("\\s", ""))
                    .filter(it->!it.isEmpty()).collect(Collectors.toList());
            Assembler.preHandleSymbol(data);
            List<String> binaryCodes = data.stream().
                    filter(it->!it.startsWith("(")).
                    map(it-> Assembler.getParser(it).parseLine(it)).
                    collect(Collectors.toList());
            Files.write(Paths.get(new File(args[1]).toURI()), binaryCodes, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("输入路径错误");
        }
    }


}


