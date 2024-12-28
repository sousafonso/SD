# Variáveis
JAVAC = javac
JAVA = java
SRC = Servidor.java Cliente.java InterfaceCliente.java BibliotecaCliente.java
CLASSES = $(SRC:.java=.class)

# Alvos principais
all: $(CLASSES)

Servidor.class: Servidor.java
	$(JAVAC) Servidor.java

Cliente.class: Cliente.java
	$(JAVAC) Cliente.java

InterfaceCliente.class: InterfaceCliente.java
	$(JAVAC) InterfaceCliente.java

BibliotecaCliente.class: BibliotecaCliente.java
	$(JAVAC) BibliotecaCliente.java

# Executar o servidor
servidor: Servidor.class
	$(JAVA) Servidor

# Executar o cliente
cliente: InterfaceCliente.class
	$(JAVA) InterfaceCliente

# Limpar ficheiros .class
clean:
	rm -f *.class
