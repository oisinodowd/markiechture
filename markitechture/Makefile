JC = javac
JVM = java
SRC_DIR = src
BIN_DIR = bin
MAIN = Main

all: compile

compile:
	@mkdir -p $(BIN_DIR)
	$(JC) -d $(BIN_DIR) -sourcepath $(SRC_DIR) $(SRC_DIR)/*.java

run: compile
	@$(JVM) -cp $(BIN_DIR) $(MAIN)

clean:
	rm -rf $(BIN_DIR)/*.class

.PHONY: all compile run clean
