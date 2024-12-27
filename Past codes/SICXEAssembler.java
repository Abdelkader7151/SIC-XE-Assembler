import java.io.*;
import java.util.*;

public class SICXEAssembler {
    // Data structures
    static List<String[]> lines = new ArrayList<>(); // Stores {label, instruction, reference}
    static Map<String, Integer> symbolTable = new HashMap<>();
    static List<Integer> locctrList = new ArrayList<>();
    private static final String[][] OPTAB = new String[59][3];
    private static final List<String> objectCodes = new ArrayList<>();
    static int startAddress = 0;

    public static void main(String[] args) throws IOException {
        parseInput("inSICXE.txt");
        initializeOPTAB();
        pass1();
        pass2();
        printResults();
        printSymbolTable();
    }

    static void parseInput(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            // Split the line into label, instruction, reference
            String[] parts = new String[3];
            String[] tokens = line.split("\\s+");
            if (tokens.length == 1) { // Only instruction
                parts[1] = tokens[0];
            } else if (tokens.length == 2) { // Instruction and reference
                parts[1] = tokens[0];
                parts[2] = tokens[1];
            } else { // Label, instruction, and reference
                parts[0] = tokens[0];
                parts[1] = tokens[1];
                parts[2] = tokens[2];
            }
            lines.add(parts);
        }
        reader.close();
    }

    static void pass1() {
        int locctr = 0;
        String baseRegisterLabel = null; // Stores the label of the base register
        String baseRegisterValue = null; // Stores the hexadecimal value of the base register

        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i);
            String label = line[0];
            String instruction = line[1];
            String reference = line[2];

            // Start directive sets the initial LOCCTR
            if ("START".equals(instruction)) {
                if (reference != null) {
                    locctr = Integer.parseInt(reference, 16);
                    startAddress = locctr;
                } else {
                    System.err.println("Error: START directive is missing a reference address.");
                    return; // Exit or handle error as required
                }
                locctrList.add(locctr);
                continue;
            }

            // Add LOCCTR to the list before updating
            locctrList.add(locctr);

            // Handle BASE instruction
            if ("BASE".equals(instruction)) {
                if (reference != null) {
                    baseRegisterLabel = reference; // Save the base register label
                    if (symbolTable.containsKey(reference)) {
                        baseRegisterValue = Integer.toHexString(symbolTable.get(reference)).toUpperCase();
                    }
                } else {
                    System.err.println("Error: BASE directive is missing a reference label.");
                }
                continue; // No change to LOCCTR for BASE
            }

            // Add label to the symbol table
            if (label != null && !label.isEmpty()) {
                if (symbolTable.containsKey(label)) {
                    System.err.println("Duplicate symbol: " + label);
                } else {
                    symbolTable.put(label, locctr);
                }

                // Update base register value if it matches the current label
                if (label.equals(baseRegisterLabel)) {
                    baseRegisterValue = Integer.toHexString(locctr).toUpperCase();
                }
            }

            // Handle special cases for instructions
            if ("END".equals(instruction)) break;

            if (instruction.startsWith("+")) { // Format 4
                locctr += 4;
            } else if (Arrays.asList("RESW", "RESB").contains(instruction)) {
                if ("RESW".equals(instruction) && reference != null) {
                    locctr += 3 * Integer.parseInt(reference);
                } else if ("RESB".equals(instruction) && reference != null) {
                    locctr += Integer.parseInt(reference);
                } else {
                    System.err.println("Error: RESW or RESB directive is missing a reference value.");
                }
            } else if ("BYTE".equals(instruction)) {
                if (reference != null) {
                    if (reference.startsWith("C")) {
                        locctr += reference.length() - 3; // C'EOF' -> 3 bytes
                    } else if (reference.startsWith("X")) {
                        locctr += (reference.length() - 3) / 2; // X'F1' -> 1 byte
                    } else {
                        System.err.println("Error: Invalid BYTE directive format.");
                    }
                } else {
                    System.err.println("Error: BYTE directive is missing a reference value.");
                }
            } else if (Arrays.asList("CLEAR", "COMPR", "TIXR", "ADDR", "DIVR", "MULR", "RMO", "SHIFTL", "SHIFTR", "SUBR", "SVC").contains(instruction)) {
                locctr += 2; // Format 2 instructions
            } else {
                locctr += 3; // Default Format 3
            }
        }

        // Print the base register details (optional)
        if (baseRegisterLabel != null && baseRegisterValue != null) {
            System.out.println("Base Register Label: " + baseRegisterLabel);
            System.out.println("Base Register Value: " + baseRegisterValue);
        }
    }



    public static void initializeOPTAB() {
        OPTAB[0] = new String[] {"FIX", "1", "C4"};
        OPTAB[1] = new String[] {"FLOAT", "1", "C0"};
        OPTAB[2] = new String[] {"HIO", "1", "F4"};
        OPTAB[3] = new String[] {"NORM", "1", "C8"};
        OPTAB[4] = new String[] {"SIO", "1", "F0"};
        OPTAB[5] = new String[] {"TIO", "1", "F8"};
        OPTAB[6] = new String[] {"ADDR", "2", "90"};
        OPTAB[7] = new String[] {"CLEAR", "2", "B4"};
        OPTAB[8] = new String[] {"COMPR", "2", "A0"};
        OPTAB[9] = new String[] {"DIVR", "2", "9C"};
        OPTAB[10] = new String[] {"MULR", "2", "98"};
        OPTAB[11] = new String[] {"RMO", "2", "AC"};
        OPTAB[12] = new String[] {"SHIFTL", "2", "A4"};
        OPTAB[13] = new String[] {"SHIFTR", "2", "A8"};
        OPTAB[14] = new String[] {"SUBR", "2", "94"};
        OPTAB[15] = new String[] {"SVC", "2", "B0"};
        OPTAB[16] = new String[] {"TIXR", "2", "B8"};
        OPTAB[17] = new String[] {"ADD", "3", "18"};
        OPTAB[18] = new String[] {"ADDF", "3", "58"};
        OPTAB[19] = new String[] {"AND", "3", "40"};
        OPTAB[20] = new String[] {"COMP", "3", "28"};
        OPTAB[21] = new String[] {"COMPF", "3", "88"};
        OPTAB[22] = new String[] {"DIV", "3", "24"};
        OPTAB[23] = new String[] {"DIVF", "3", "64"};
        OPTAB[24] = new String[] {"J", "3", "3C"};
        OPTAB[25] = new String[] {"JEQ", "3", "30"};
        OPTAB[26] = new String[] {"JGT", "3", "34"};
        OPTAB[27] = new String[] {"JLT", "3", "38"};
        OPTAB[28] = new String[] {"JSUB", "3", "48"};
        OPTAB[29] = new String[] {"LDA", "3", "00"};
        OPTAB[30] = new String[] {"LDB", "3", "68"};
        OPTAB[31] = new String[] {"LDCH", "3", "50"};
        OPTAB[32] = new String[] {"LDF", "3", "70"};
        OPTAB[33] = new String[] {"LDL", "3", "08"};
        OPTAB[34] = new String[] {"LDS", "3", "6C"};
        OPTAB[35] = new String[] {"LDT", "3", "74"};
        OPTAB[36] = new String[] {"LDX", "3", "04"};
        OPTAB[37] = new String[] {"LPS", "3", "D0"};
        OPTAB[38] = new String[] {"MUL", "3", "20"};
        OPTAB[39] = new String[] {"MULF", "3", "60"};
        OPTAB[40] = new String[] {"OR", "3", "44"};
        OPTAB[41] = new String[] {"RD", "3", "D8"};
        OPTAB[42] = new String[] {"RSUB", "3", "4C"};
        OPTAB[43] = new String[] {"SSK", "3", "EC"};
        OPTAB[44] = new String[] {"STA", "3", "0C"};
        OPTAB[45] = new String[] {"STB", "3", "78"};
        OPTAB[46] = new String[] {"STCH", "3", "54"};
        OPTAB[47] = new String[] {"STF", "3", "80"};
        OPTAB[48] = new String[] {"STI", "3", "D4"};
        OPTAB[49] = new String[] {"STL", "3", "14"};
        OPTAB[50] = new String[] {"STS", "3", "7C"};
        OPTAB[51] = new String[] {"STSW", "3", "E8"};
        OPTAB[52] = new String[] {"STT", "3", "84"};
        OPTAB[53] = new String[] {"STX", "3", "10"};
        OPTAB[54] = new String[] {"SUB", "3", "1C"};
        OPTAB[55] = new String[] {"SUBF", "3", "5C"};
        OPTAB[56] = new String[] {"TD", "3", "E0"};
        OPTAB[57] = new String[] {"TIX", "3", "2C"};
        OPTAB[58] = new String[] {"WD", "3", "DC"};
    }

    private static int getRegisterNumber(String register) {
        switch (register.toUpperCase()) {
            case "A": return 0;
            case "X": return 1;
            case "L": return 2;
            case "B": return 3;
            case "S": return 4;
            case "T": return 5;
            case "F": return 6;
            default: return 0;
        }
    }

    private static String binaryToHex(String binary) {
        return Integer.toHexString(Integer.parseInt(binary, 2)).toUpperCase();
    }

    public static void pass2() {
        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i);
            String instruction = line[1];
            String reference = line[2];
            String objectCode = "";

            // Skip directives that don't produce object code
            if ("END".equals(instruction) || "RESW".equals(instruction) || "RESB".equals(instruction) || "BASE".equals(instruction) || "START".equals(instruction)) {
                objectCodes.add(objectCode);
                continue;
            }

            for (String[] opt : OPTAB) {
                if (opt[0].equals(instruction.replace("+", ""))) {
                    int format = Integer.parseInt(opt[1]);
                    String opcode = opt[2];

                    // ----------------------- Format 1 ----------------------------------
                    // Format 1 has no operands, and the object code is just the opcode.
                    if (format == 1) {
                        objectCode = opcode;
                    }

                    // ----------------------- Format 2 ----------------------------------
                    // Format 2 uses two registers, which are represented in the object code.
                    else if (format == 2) {
                        String[] registers = reference.split(",");
                        int r1 = getRegisterNumber(registers[0]);
                        int r2 = registers.length > 1 ? getRegisterNumber(registers[1]) : 0;
                        objectCode = opcode + String.format("%X%X", r1, r2);
                    }

                    // ----------------------- Format 3 & 4 ------------------------------
                    // Format 3/4 involves addressing modes and memory references.
                    else if (format == 3 || format == 4) {
                        boolean isExtended = instruction.startsWith("+");
                        int targetAddress = 0;
                        int displacement = 0;

                        String nixbpe = "110000"; // Default addressing mode (n=1, i=1, x=0, b=0, p=0, e=0)

                        // Immediate addressing
                        if (reference.startsWith("#")) {
                            nixbpe = "010000";
                            String operand = reference.substring(1);

                            // If the operand is a symbol, retrieve its address from the symbol table
                            targetAddress = symbolTable.containsKey(operand) ? symbolTable.get(operand) : Integer.parseInt(operand);
                        }
                        // Indirect addressing
                        else if (reference.startsWith("@")) {
                            nixbpe = "100000";
                            targetAddress = symbolTable.get(reference.substring(1));
                        }
                        // Simple addressing
                        else {
                            targetAddress = symbolTable.getOrDefault(reference, 0);
                        }

                        // If indexed addressing is specified
                        if (reference.contains(",X")) {
                            nixbpe = "111000";
                            reference = reference.replace(",X", "");
                        }

                        // Calculate displacement or full address based on format
                        if (!isExtended) {
                            displacement = targetAddress - (locctrList.get(i + 1));
                            if (displacement >= -2048 && displacement <= 2047) {
                                nixbpe = nixbpe.substring(0, 4) + "10"; // Set p=1, b=0
                            } else {
                                // Base-relative addressing
                                //displacement = targetAddress - symbolTable.get("BASE");
                                nixbpe = nixbpe.substring(0, 4) + "01"; // Set p=0, b=1
                            }
                            objectCode = opcode + binaryToHex(nixbpe) + String.format("%03X", displacement & 0xFFF);
                        } else {
                            // Format 4 uses full 20-bit address
                            nixbpe = nixbpe.substring(0, 4) + "01"; // Set e=1
                            objectCode = opcode + binaryToHex(nixbpe) + String.format("%05X", targetAddress);
                        }
                    }}}

            // ----------------------- Handling BYTE Instructions -------------------
            // Handle BYTE instructions which can either be in character or hexadecimal format.
            if ("BYTE".equals(instruction)) {
                if (reference.startsWith("C'")) {
                    String chars = reference.substring(2, reference.length() - 1);
                    StringBuilder hex = new StringBuilder();
                    for (char c : chars.toCharArray()) {
                        hex.append(String.format("%02X", (int) c));
                    }
                    objectCode = hex.toString();
                } else if (reference.startsWith("X'")) {
                    objectCode = reference.substring(2, reference.length() - 1);
                }
            }

            // ----------------------- Handling WORD Instructions -------------------
            // Handle WORD instructions where the reference is simply converted to a 6-digit hexadecimal number.
            else if ("WORD".equals(instruction)) {
                objectCode = String.format("%06X", Integer.parseInt(reference));
            }

            // Add the object code to the list
            objectCodes.add(objectCode);
        }
    }








    public static void printResults() {
        System.out.printf("%-13s %-13s %-13s %-13s %-13s\n", "Label", "Instruction", "Reference", "LOCCTR", "Object Code");
        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i);
            String locctr = locctrList.get(i) != null ? String.format("%04X", locctrList.get(i)) : "";
            String objectCode = objectCodes.get(i);
            System.out.printf("%-13s %-13s %-13s %-13s %-13s\n", line[0], line[1], line[2], locctr, objectCode);
        }
    }



    static void printSymbolTable() {
        System.out.println("\nSymbol Table:");
        System.out.printf("%-10s %-10s\n", "-Label-", "-Address-");
        for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
            String address = Integer.toHexString(entry.getValue()).toUpperCase();
            System.out.printf("%-10s %-10s\n", entry.getKey(), address);
        }
    }
}
