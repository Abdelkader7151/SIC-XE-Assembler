package sicxe;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            String inputFile = "inSICXE.txt";

            SICXEAssembler assembler = new SICXEAssembler();
            assembler.assemble(inputFile);
            assembler.printResults();

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}