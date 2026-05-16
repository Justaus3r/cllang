SRC_DIR = ./src/CLLang/
JJTREE_FILE = cllang.jjt
GRAMMER_FILE = cllang.jj 
CLASS_NAME = CLLang

all: build


build:
	@echo Building parser...
	cd $(SRC_DIR); \
	jjtree $(JJTREE_FILE) ; \
	javacc $(GRAMMER_FILE) ; \
	javac *.java

test:
	@echo Testing source file with the parser ..
	@cd $(SRC_DIR); \
	java CLLang.java < ../../tests/cllang.source ; \
	[ $$? != 0 ] && echo "[FAILURE] Parsing Failed! " || echo "[SUCCESS] Parsing Passed!"

clean:
	# if conditional fails the retcode is 1 and it fucks up the flow, the || provides shorcircuit to true to keep retcode 0
	ls $(SRC_DIR) | grep .class && rm $(SRC_DIR)*.class || true 
	mv $(SRC_DIR)SymbolTable.java $(SRC_DIR)SymbolTable
	mv $(SRC_DIR)SemanticAnalysis.java $(SRC_DIR)SemanticAnalysis
	mv $(SRC_DIR)IRGeneration.java $(SRC_DIR)IRGeneration
	mv $(SRC_DIR)AsmGeneration.java $(SRC_DIR)AsmGeneration
	
	ls $(SRC_DIR) | grep .java && rm $(SRC_DIR)*.java || true
	
	mv $(SRC_DIR)SymbolTable $(SRC_DIR)SymbolTable.java
	mv $(SRC_DIR)SemanticAnalysis $(SRC_DIR)SemanticAnalysis.java
	mv $(SRC_DIR)IRGeneration $(SRC_DIR)IRGeneration.java
	mv $(SRC_DIR)AsmGeneration $(SRC_DIR)AsmGeneration.java

	ls $(SRC_DIR) | grep .jj && rm $(SRC_DIR)*.jj || true

eof:
