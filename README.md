# SIC/XE Assembler

A Java implementation of a SIC/XE assembler with Pass 1 (symbols/locations) and Pass 2 (object code + HTE records).

## 📊 Overview
- Reads SIC/XE assembly input (e.g., `inSICXE.txt`)
- Pass 1: parses directives/opcodes, tracks formats (2/3/4), builds symbol table and location counters
- Pass 2: computes nixbpe flags, PC/BASE-relative displacements, generates object code
- Outputs H/T/E records for the program

## 💻 Build & Run
```bash
javac -d out .\src\sicxe\*.java
java -cp out sicxe.Main
```

## 🗂️ Structure
- `src/sicxe/Main.java` — entry
- `src/sicxe/SICXEAssembler.java` — core logic
- `src/sicxe/Converter.java`, `Data.java`, `Pair.java`
- `inSICXE.txt`, `inSICXE2.txt`

---

## 👨‍💻 Author

**Abdelrhman Abdelkader**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Abdelkader7151)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/abdelrhman-abdelkader-6313a4291/)

