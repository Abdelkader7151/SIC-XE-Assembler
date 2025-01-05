package sicxe;

import java.io.*;
import java.util.*;

public class SICXEAssembler {
    private final ArrayList<Data> data;
    private final ArrayList<String> length;
    private final ArrayList<String> location;
    private final ArrayList<Pair> symTab;
    private final ArrayList<String> target;
    private String base = "";

    public SICXEAssembler() {
        Converter.initialize();
        data = new ArrayList<>();
        length = new ArrayList<>();
        location = new ArrayList<>();
        symTab = new ArrayList<>();
        target = new ArrayList<>();
    }

    public void assemble(String inputFile) throws IOException {
        readInputFile(inputFile);
        processLocations();
        generateObjectCode();
    }

    private void readInputFile(String inputFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            Scanner scn = new Scanner(br);
            processInputLines(scn);
        }
    }

    private void processInputLines(Scanner scn) {
        int len = 0;
        String str1 = " ", str2 = " ", str3 = " ", op = " ", format = " ";
        boolean isOpCode = false, isLine = false;

        while (scn.hasNext()) {
            String tempString = scn.next();

            // Check if the token is a directive
            if (tempString.equals("START") || tempString.equals("END") ||
                    tempString.equals("WORD") || tempString.equals("BYTE") ||
                    tempString.equals("RESB") || tempString.equals("RESW") ||
                    tempString.equals("BASE")) {
                str2 = tempString;
                isOpCode = true;
            } else {
                String s = tempString;

                // Handle extended format opcodes
                if (tempString.contains("+")) {
                    s = tempString.substring(1);
                    len = 1; // Indicates extended format
                }

                // Check if the token is a valid opcode
                String[] opcode = Converter.getOpcode(s);
                if (opcode != null) {
                    str2 = tempString;
                    op = opcode[2];
                    len += Integer.parseInt(opcode[1]); // Add the opcode length
                    format = Integer.toString(len);
                    isOpCode = true;
                }
            }

            if (tempString.equals(".")) {
                str1 = ".";
                isLine = true;
            }

            // Automatically process lines with RSUB
            if (str2.equals("RSUB")) {
                isLine = true;
            }

            // Handle labels
            if (!isOpCode) {
                str1 = tempString;
            } else if (!str2.equals("RSUB")) { // Process operands if not RSUB
                str3 = scn.next();
                isLine = true;
            }

            // Add the line to data if ready
            if (isLine) {
                data.add(new Data(str1, str2, str3, str1 + str2 + str3, op, format));
                length.add(Integer.toString(len));
                // Reset variables for the next line
                str1 = " ";
                str2 = " ";
                str3 = " ";
                len = 0;
                op = " ";
                format = " ";
                isLine = false;
                isOpCode = false;
            }
        }
    }


    private void processLocations() {
        int decLoc = 0;  // Decimal location counter
        String hexLoc;   // Current location in hexadecimal

        for (int i = 0; i < data.size(); i++) {
            // Check for comments
            if (data.get(i).str.contains(".") || data.get(i).str.contains("BASE")) {
                if (data.get(i).str.contains("BASE")) {
                    base = data.get(i).third;  // Update base address
                }
            }

            // Calculate the location for the current instruction
            if (i > 0) {
                // Update decimal location based on the length of the previous instruction
                decLoc += Integer.parseInt(length.get(i - 1), 16);
            }

            // Format the current location as hexadecimal
            hexLoc = String.format("%04X", decLoc);  // Ensure it's always 4 digits
            location.add(hexLoc);  // Add the current location to the list

            // Handle special cases for instruction lengths
            updateLengthForSpecialCases(i);
            addToSymbolTable(i, hexLoc);
        }

        // Ensure the last location is correctly set
        if (!location.isEmpty()) {
            String lastLocation = location.get(location.size() - 1);
            // If the last location is empty, set it to the last computed location
            if (lastLocation.isEmpty() && !length.isEmpty()) {
                decLoc += Integer.parseInt(length.get(length.size() - 1), 16);  // Add the last instruction's length
                hexLoc = String.format("%04X", decLoc);  // Format as 4-digit hex
                location.set(location.size() - 1, hexLoc);  // Update last location
            }
        }
    }


    private void updateLengthForSpecialCases(int i) {
        if (data.get(i).second.equals("BASE")) {
            length.set(i, "0");  // Set length to 0 for BASE directive
        } else if (data.get(i).second.equals("BYTE")) {
            if (data.get(i).third.contains("C")) {
                char[] c = data.get(i).third
                        .substring(data.get(i).third.indexOf('\'') + 1, data.get(i).third.length() - 1)
                        .toCharArray();
                length.set(i, Integer.toString(c.length));  // Set length based on character count
            } else {
                length.set(i, "1");  // For X or other BYTE formats
            }
        } else if (data.get(i).second.equals("RESW")) {
            length.set(i, Integer.toHexString(Integer.parseInt(data.get(i).third) * 3));  // 3 bytes per word
        } else if (data.get(i).second.equals("RESB")) {
            length.set(i, Integer.toHexString(Integer.parseInt(data.get(i).third)));  // Reserve bytes
        } else if (data.get(i).second.equals("WORD")) {
            length.set(i, "3");  // WORD occupies 3 bytes
        } else if (data.get(i).second.equals("CLEAR")) {
            length.set(i, "2");  // CLEAR occupies 2 bytes
        }
    }

    private void addToSymbolTable(int i, String hexLoc) {
        if (!data.get(i).first.contains(" ") && i != 0 && !data.get(i).first.contains(".")) {
            if (data.get(i).first.equals(base))
                base = hexLoc;
            symTab.add(new Pair(data.get(i).first, hexLoc));
        }
    }

    private void generateObjectCode() {
        for (int i = 0; i < data.size(); i++) {
            StringBuilder s = new StringBuilder();

            if (data.get(i).format.equals("2")) {
                processFormat2(data.get(i), s);
            }
            if (data.get(i).format.equals("4")) {
                processFormat4(data.get(i), s);
            }
            if (data.get(i).format.equals("3")) {
                processFormat3(data.get(i), s, i);
            }
            if (data.get(i).second.equals("BYTE")) {
                processByteDirective(data.get(i), s);
            }
            if (data.get(i).second.equals("WORD")) {
                processWordDirective(data.get(i), s);
            }

            target.add(s.toString());
        }
    }

    private void processFormat2(Data currentData, StringBuilder s) {
        s.append(currentData.opcode);
        String[] arr = currentData.third.split(",");
        for (int k = 0; k < arr.length; k++) {
            switch (arr[k]) {
                case "B":
                    s.append("3");
                    break;
                case "S":
                    s.append("4");
                    break;
                case "T":
                    s.append("5");
                    break;
                case "F":
                    s.append("6");
                    break;
                case "A":
                    s.append("0");
                    break;
                case "X":
                    s.append("1");
                    break;
            }
        }
        if (arr.length == 1) s.append("0");
    }



    private void processFormat4(Data currentData, StringBuilder s) {
        String str = "", nixbpe = "";
        str = Integer.toBinaryString(Integer.parseInt(currentData.opcode, 16));
        if(str.equals("0"))
            str = "000000"; // LDA needs leading zeros since opcode=0
        if(currentData.third.contains("#"))
            nixbpe = "010001";
        else if(currentData.third.contains("@"))
            nixbpe = "100001";
        else if(currentData.third.contains(",X"))
            nixbpe = "111001";
        else
            nixbpe = "110001";

        str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
        if(str.length() != 3) { // For LDA, pad with 0
            str = new StringBuilder(str).reverse().append("0").reverse().toString();
        }

        for(int k = 0; k < symTab.size(); k++) {
            if(currentData.third.contains(symTab.get(k).symbol)) {
                if(symTab.get(k).location.length() != 5) { // Pad symbol location to 5 digits
                    for(int l = symTab.get(k).location.length(); l < 5; l++)
                        str += "0";
                }
                str += symTab.get(k).location;
                break;
            }
        }

        char[] c = currentData.third.toCharArray();
        if((c[1]-'0' >= 0 && c[1]-'0' <= 9)) { // Convert immediate value (#4096) to hex
            String hex = Integer.toHexString(Integer.parseInt(currentData.third.substring(1)));
            if(hex.length() != 5) { // Pad hex to 5 digits
                for(int l = hex.length(); l < 5; l++)
                    str += "0";
            }
            str += hex;
        }
        s.append(str);
    }

    private void processFormat3(Data currentData, StringBuilder s, int i) {
        String str = "", nixbpe = "";
        str = Integer.toBinaryString(Integer.parseInt(currentData.opcode, 16));
        if(str.equals("0"))
            str = "000000";

        if(currentData.third.equals(" ") || currentData.second.equals("RSUB")) {
            nixbpe = "110000";
            str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
            str += "000";
        }
        else if(currentData.third.contains(",X")) {
            String num = "", num2 = location.get(i+1);
            int tot = 0;
            for(int k = 0; k < symTab.size(); k++) {
                if(currentData.third.contains(symTab.get(k).symbol)) {
                    num = symTab.get(k).location;
                    break;
                }
            }
            if(num2.equals(""))
                num2 = location.get(i+2);
            tot = Integer.parseInt(num, 16) - Integer.parseInt(num2, 16);
            if(tot < 2047 && tot > -2048) {
                nixbpe = "111010";
                str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
                if(str.length() != 3) {
                    str = new StringBuilder(str).reverse().append("0").reverse().toString();
                }
                if(Integer.toHexString(tot).length() != 3) {
                    for(int l = Integer.toHexString(tot).length(); l < 3; l++)
                        str += "0";
                }
                str += Integer.toHexString(tot).toUpperCase();
            } else {
                nixbpe = "111100";
                tot = Integer.parseInt(num, 16) - Integer.parseInt(base, 16);
                str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
                if(str.length() != 3) {
                    str = new StringBuilder(str).reverse().append("0").reverse().toString();
                }
                if(Integer.toHexString(tot).length() != 3) {
                    for(int l = Integer.toHexString(tot).length(); l < 3; l++)
                        str += "0";
                }
                str += Integer.toHexString(tot).toUpperCase();
            }
        }
        else if(currentData.third.contains("#") || currentData.third.contains("@")) {
            char[] c = currentData.third.toCharArray();
            if((c[1]-'0' >= 0 && c[1]-'0' <= 9)) {
                String num = currentData.third.substring(1);
                num = Integer.toHexString(Integer.parseInt(num)).toUpperCase();
                if(currentData.third.contains("#"))
                    nixbpe = "010000";
                else
                    nixbpe = "100000";
                str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
                if(str.length() != 3) {
                    str = new StringBuilder(str).reverse().append("0").reverse().toString();
                }
                if(num.length() != 3) {
                    for(int l = num.length(); l < 3; l++)
                        str += "0";
                }
                str += num;
            } else {
                String num = "", num2 = location.get(i+1);
                int tot = 0;
                for(int k = 0; k < symTab.size(); k++) {
                    if(currentData.third.contains(symTab.get(k).symbol)) {
                        num = symTab.get(k).location;
                        break;
                    }
                }
                if(num2.equals(""))
                    num2 = location.get(i+2);
                tot = Integer.parseInt(num, 16) - Integer.parseInt(num2, 16);
                if(tot < 2047 && tot > -2048) {
                    if(currentData.third.contains("#"))
                        nixbpe = "010010";
                    else
                        nixbpe = "100010";
                    str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
                    if(str.length() != 3) {
                        str = new StringBuilder(str).reverse().append("0").reverse().toString();
                    }
                    if(Integer.toHexString(tot).length() != 3) {
                        for(int l = Integer.toHexString(tot).length(); l < 3; l++)
                            str += "0";
                    }
                    str += Integer.toHexString(tot).toUpperCase();
                } else {
                    if(currentData.third.contains("#"))
                        nixbpe = "010100";
                    else
                        nixbpe = "100100";
                    tot = Integer.parseInt(num, 16) - Integer.parseInt(base, 16);
                    str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
                    if(str.length() != 3) {
                        str = new StringBuilder(str).reverse().append("0").reverse().toString();
                    }
                    if(Integer.toHexString(tot).length() != 3) {
                        for(int l = Integer.toHexString(tot).length(); l < 3; l++)
                            str += "0";
                    }
                    str += Integer.toHexString(tot).toUpperCase();
                }
            }
        }
        else {
            String num = "", num2 = location.get(i+1);
            int tot = 0;
            for(int k = 0; k < symTab.size(); k++) {
                if(currentData.third.contains(symTab.get(k).symbol)) {
                    num = symTab.get(k).location;
                    break;
                }
            }
            tot = Integer.parseInt(num, 16) - Integer.parseInt(num2, 16);
            if(tot < 2047 && tot > -2048) {
                nixbpe = "110010";
                str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
                if(str.length() != 3) {
                    str = new StringBuilder(str).reverse().append("0").reverse().toString();
                }
                if(Integer.toHexString(tot).length() < 3) {
                    for(int l = Integer.toHexString(tot).length(); l < 3; l++)
                        str += "0";
                }
                if(Integer.toHexString(tot).length() > 3) {
                    StringBuffer s1 = new StringBuffer(Integer.toHexString(tot).toUpperCase());
                    str += new StringBuffer(s1.reverse().substring(0,3)).reverse();
                }
                else
                    str += Integer.toHexString(tot).toUpperCase();
            } else {
                nixbpe = "110100";
                tot = Integer.parseInt(num, 16) - Integer.parseInt(base, 16);
                str = Integer.toHexString(Integer.parseInt((str.substring(0, str.length()-2) + nixbpe), 2)).toUpperCase();
                if(str.length() != 3) {
                    str = new StringBuilder(str).reverse().append("0").reverse().toString();
                }
                if(Integer.toHexString(tot).length() != 3) {
                    for(int l = Integer.toHexString(tot).length(); l < 3; l++)
                        str += "0";
                }
                if(Integer.toHexString(tot).length() > 3) {
                    StringBuffer s1 = new StringBuffer(Integer.toHexString(tot).toUpperCase());
                    str += new StringBuffer(s1.reverse().substring(0,3)).reverse();
                }
                else
                    str += Integer.toHexString(tot).toUpperCase();
            }
        }
        s.append(str);
    }

    private void processByteDirective(Data currentData, StringBuilder s) {
        char[] c = currentData.third
                .substring(currentData.third.indexOf('\'') + 1, currentData.third.length() - 1)
                .toCharArray();
        for (char ch : c) {
            if (currentData.third.contains("C"))
                s.append(Integer.toHexString(ch).toUpperCase());
            else
                s.append(ch);
        }
    }

    private void processWordDirective(Data currentData, StringBuilder s) {
        // Extract the number after the WORD directive
        String wordValue = currentData.third.trim();

        // Convert the number to hexadecimal
        try {
            int num = Integer.parseInt(wordValue);  // Assuming the word is a decimal number
            String hexValue = Integer.toHexString(num).toUpperCase();

            // Ensure the hexadecimal value is 6 characters long (pad with zeros if needed)
            while (hexValue.length() < 6) {
                hexValue = "0" + hexValue;
            }

            // Append the resulting hexadecimal value to the object code
            s.append(hexValue);
        } catch (NumberFormatException e) {
            // Handle the case where the value is not a valid integer (e.g., an error in the input)
            System.err.println("Invalid number format after WORD directive: " + wordValue);
        }
    }


    private void printAssemblyResults() {
        System.out.println("**************************Pass1 and Pass2**************************");
        System.out.printf("%-13s %-13s %-13s %-13s %-13s\n", "LOCCTR", "Label", "Instruction", "Reference", "Object Code");
        System.out.println("-------------------------------------------------------------------");
        for (int j = 0; j < data.size(); j++) {
            System.out.printf("%-13s %-13s %-13s %-13s %-13s\n" , location.get(j), data.get(j).first, data.get(j).second, data.get(j).third, target.get(j));
        }
        System.out.println("-------------------------------------------------------------------");
    }

    private void generateHTERecords() {
        StringBuilder h = new StringBuilder();
        ArrayList<StringBuilder> t = new ArrayList<>();
        StringBuilder e = new StringBuilder();
        StringBuilder currentT = new StringBuilder();
        int startAddr = 0;
        int currentTLength = 0;

        // Generate H record
        h.append("H").append("^");
        // Get program name from first line (padded to 6 chars)
        String progName = data.get(0).first.trim();
        progName += "^";
        h.append(progName);
        // Starting address (6 chars)
        String startAddrHex = String.format("%06X", Integer.parseInt(location.get(0), 16));
        h.append(startAddrHex);
        // Program length (6 chars) - calculate from last location counter
        int lastIdx = location.size() - 1;
        while (location.get(lastIdx).isEmpty()) lastIdx--;
        int progLength = Integer.parseInt(location.get(lastIdx), 16) - Integer.parseInt(location.get(0), 16);
        h.append(String.format("^%06X", progLength));

        // Generate T records
        startAddr = Integer.parseInt(location.get(0), 16);
        currentT.append("T").append(String.format("%06X", startAddr));

        for (int i = 0; i < data.size(); i++) {
            if (location.get(i).isEmpty() || target.get(i).isEmpty()) continue;

            int objCodeLength = target.get(i).length() / 2; // Two hex chars = 1 byte

            // If adding this object code would exceed 30 bytes or if there's a RESW/RESB,
            // start a new T record
            if (currentTLength + objCodeLength > 30 ||
                    data.get(i).second.equals("RESW") ||
                    data.get(i).second.equals("RESB")) {
                if (currentTLength > 0) {
                    // Add length byte to existing T record and save it
                    currentT.insert(7, String.format("%02X", currentTLength));
                    t.add(new StringBuilder(currentT));

                    // Start new T record
                    currentT = new StringBuilder("T");
                    if (i < location.size()) {
                        currentT.append(String.format("%06X", Integer.parseInt(location.get(i), 16)));
                    }
                    currentTLength = 0;
                }
            }

            if (!data.get(i).second.equals("RESW") && !data.get(i).second.equals("RESB")) {
                currentT.append(target.get(i));
                currentTLength += objCodeLength;
            }
        }

        // Add final T record if not empty
        if (currentTLength > 0) {
            currentT.insert(7, String.format("%02X", currentTLength));
            t.add(new StringBuilder(currentT));
        }

        // Generate E record
        e.append("E^").append(startAddrHex);

        // Print HTE records
        System.out.println("******************* HTE records *******************");
        System.out.println(h.toString());
        for (StringBuilder tRecord : t) {
            System.out.println(tRecord.toString());
        }
        System.out.println(e.toString());
    }

    public void printSymTab() {
            // Print headers
            System.out.printf("%-15s %-10s\n", "Symbol", "Location");
            System.out.println("----------------------------");

            // Iterate through the symbol table and print each entry
            for (Pair pair : symTab) {
                System.out.printf("%-15s %-10s\n", pair.symbol, pair.location);
            }
            System.out.println("-------------------------------------------------------------------");
    }



    public void printResults() {
        printAssemblyResults();
        printSymTab();
        generateHTERecords();
    }


}