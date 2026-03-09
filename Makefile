SRC_DIR = ./src/CLLang/
JJTREE_FILE = cllang.jjt
GRAMMER_FILE = cllang.jj 
CLASS_NAME = CLLang

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
	@echo Cleaning build files...
	rm $(SRC_DIR)*.class
	mv $(SRC_DIR)SymbolTable.java $(SRC_DIR)SymbolTable
	rm $(SRC_DIR)*.java
	mv $(SRC_DIR)SymbolTable $(SRC_DIR)SymbolTable.java
	rm $(SRC_DIR)*.jj
