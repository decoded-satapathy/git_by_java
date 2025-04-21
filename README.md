# git_lite Project

## Overview
`git_lite` is a simplified version of Git, built to help understand the core concepts behind version control systems. This project allows basic Git-like functionalities, such as initializing repositories, committing changes, and viewing logs.

## Prerequisites
Before running the project, ensure you have the following installed:
- **Java** (version 23 or higher)
- **Maven**
- **Bash shell** (on Linux/macOS/WSL2)

You can check if Java and Maven are installed using the following commands:
```bash
java -version
mvn -version
```

---

## Running the Project

### For Linux/macOS/WSL2 Users
This project includes a shell script `your_program.sh` to simplify the build and execution process.

1. **Clone the Repository**
```bash
git clone https://github.com/decoded-satapathy/git_by_java.git
cd git_lite
```

2. **Make the Script Executable**
```bash
chmod +x your_program.sh
```

3. **Clean and Build the Project**
```bash
mvn clean package -Ddir=/tmp/git_lite_build
```

4. **Run the Script**
This will:
- Navigate to the project directory.
- Build the project using Maven.
- Execute the generated jar file.
```bash
./your_program.sh
```

You can also pass arguments to it like:
```bash
./your_program.sh init
./your_program.sh commit -m "Initial commit"
```

The script essentially runs:
```bash
#!/bin/sh
set -e
(
  cd "$(dirname "$0")"
  mvn -B package -Ddir=/tmp/git_lite_build
)
exec java -jar /tmp/git_lite_build/git_lite.jar "$@"
```

### For Native Windows Users
Since `.sh` scripts do not run natively on Windows, follow these manual steps:

1. **Clone the Repository**
```powershell
git clone https://github.com/decoded-satapathy/git_by_java.git
cd git_lite
```

2. **Clean and Build the Project**
```powershell
mvn clean package -Ddir=target
```

3. **Run the JAR File**
```powershell
java -jar target/git_lite.jar
```

Or with arguments:
```powershell
java -jar target/git_lite.jar init
java -jar target/git_lite.jar commit -m "Initial commit"
```

---

## Implemented Features

### 1. `init`
Initializes a new Git repository by creating the necessary directory structure.

**Usage:**
```bash
./your_program.sh init
```

**What it does:**
- Creates a `.git` directory
- Sets up `objects` and `refs` subdirectories
- Creates a `HEAD` file pointing to the main branch

### 2. `cat-file`
Displays the contents of a Git object (blob, tree, commit) when given its SHA-1 hash.

**Usage:**
```bash
./your_program.sh cat-file -p <object_sha>
```

**What it does:**
- Locates the object file using its SHA-1 hash
- Decompresses the object content
- Displays the content after the header

### 3. `hash-object`
Computes the SHA-1 hash for a file's contents and optionally stores it as a Git object.

**Usage:**
```bash
./your_program.sh hash-object -w <file_path>
```

**What it does:**
- Reads the content of the specified file
- Computes its SHA-1 hash
- With `-w` flag, stores the object in the Git object database
- Outputs the computed hash

### 4. `ls-tree`
Lists files and subdirectories in a tree object.

**Usage:**
```bash
./your_program.sh ls-tree --name-only <tree_sha>
```

**What it does:**
- Locates and reads the tree object using its SHA
- Parses the tree structure to extract file/directory names
- Displays the names in alphabetical order

### 5. `write-tree`
Creates a tree object from the current directory structure and returns its SHA.

**Usage:**
```bash
./your_program.sh write-tree
```

**What it does:**
- Recursively traverses the current directory (excluding `.git`)
- Creates blob objects for files and tree objects for subdirectories
- Builds a tree structure representing the current state
- Outputs the SHA-1 of the created tree object

### 6. `commit-tree`
Creates a commit object from a tree SHA with optional parent commit and message.

**Usage:**
```bash
./your_program.sh commit-tree <tree_sha> -p <parent_commit_sha> -m "Commit message"
```

**What it does:**
- Creates a commit object referencing the specified tree
- Links to parent commit(s) if provided
- Includes author, committer information, and timestamp
- Stores the commit message
- Outputs the SHA-1 of the created commit

### 7. `clone`
Clones a remote Git repository.

**Usage:**
```bash
./your_program.sh clone <repository_url> <target_directory>
```

**What it does:**
- Creates a new directory for the cloned repository
- Downloads all objects and refs from the remote repository
- Sets up the local repository structure

---

## Notes
- The JAR is built to include all dependencies using the Maven Assembly Plugin.
- The output directory is `/tmp/git_lite_build` on Unix-like systems, or `target/` on Windows.
- You can customize the output directory by modifying the `-Ddir=...` argument in the script or command.

---

## License
MIT License
