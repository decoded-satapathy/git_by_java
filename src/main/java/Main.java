import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;
import org.eclipse.jgit.api.Git;

public class Main {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("No command provided.");
      System.exit(1);
    }

    final String command = args[0];

    switch (command) {
      case "init" -> initGit(); // init the git repo
      case "cat-file" -> catFile(args); // Displays the contents of a Git object (blob, tree, commit, etc.) given its
                                        // SHA-1 hash.
      case "hash-object" -> hashObject(args); // Compute and optionally save the SHA-1 hash for a file’s contents as a
                                              // Git object.
      case "ls-tree" -> lsTree(args); // List files and subtrees in a tree object, showing their type, SHA, and name.
      case "write-tree" -> writeTreeCommand(); // Write the current index as a tree object and print its SHA
      case "commit-tree" -> commitTree(args); // Create a commit object from a tree SHA, with parents and message, and
                                              // print its SHA
      case "clone" -> cloneRepo(args);// Clone the repo
      default -> System.out.println("Unknown command: " + command);
    }
  }

  private static void initGit() {
    final File root = new File(".git");
    new File(root, "objects").mkdirs();
    new File(root, "refs").mkdirs();
    final File head = new File(root, "HEAD");
    try {
      head.createNewFile();
      Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
      System.out.println("Initialized git directory");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void catFile(String[] args) {
    if (args.length < 3 || !args[1].equals("-p")) {
      System.err.println("Usage: git cat-file -p <blob_sha>");
      System.exit(1);
    }
    String blobSha = args[2];
    if (blobSha.length() != 40) {
      System.err.println("Invalid blob SHA: Must be 40 characters long.");
      System.exit(1);
    }
    Path objectPath = Paths.get(".git", "objects", blobSha.substring(0, 2), blobSha.substring(2));
    if (!Files.exists(objectPath)) {
      System.err.println("Error: Object file not found for SHA: " + blobSha);
      System.exit(1);
    }
    try (InputStream fileStream = Files.newInputStream(objectPath);
        InflaterInputStream inflaterStream = new InflaterInputStream(fileStream);
        ByteArrayOutputStream decompressedDataStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inflaterStream.read(buffer)) != -1) {
        decompressedDataStream.write(buffer, 0, bytesRead);
      }
      byte[] decompressedBytes = decompressedDataStream.toByteArray();
      int nullByteIndex = -1;
      for (int i = 0; i < decompressedBytes.length; i++) {
        if (decompressedBytes[i] == 0) {
          nullByteIndex = i;
          break;
        }
      }
      if (nullByteIndex == -1) {
        System.err.println("Invalid blob object format: Missing null separator.");
        System.exit(1);
      }
      byte[] contentBytes = Arrays.copyOfRange(decompressedBytes, nullByteIndex + 1, decompressedBytes.length);
      String content = new String(contentBytes, StandardCharsets.UTF_8);
      System.out.print(content);
    } catch (IOException e) {
      System.err.println("Error reading or accessing blob file: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void hashObject(String[] args) {
    if (args.length < 3 || !args[1].equals("-w")) {
      System.err.println("Usage: git hash-object -w <file>");
      System.exit(1);
    }
    File inputFile = new File(args[2]);
    if (!inputFile.exists() || inputFile.isDirectory()) {
      System.err.println("Error: File not found or is a directory: " + args[2]);
      System.exit(1);
    }
    try {
      byte[] contentBytes = Files.readAllBytes(inputFile.toPath());
      String header = "blob " + contentBytes.length + "\0";
      byte[] fullContent = concatBytes(header.getBytes(StandardCharsets.UTF_8), contentBytes);
      String sha1Hash = sha1Hex(fullContent);
      System.out.println(sha1Hash);
      writeObject(sha1Hash, fullContent);
    } catch (IOException | NoSuchAlgorithmException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void lsTree(String[] args) {
    if (args.length < 3 || !args[1].equals("--name-only")) {
      System.err.println("Usage: git ls-tree --name-only <tree_sha>");
      System.exit(1);
    }

    String treeSha = args[2];
    Path objectPath = Paths.get(".git", "objects", treeSha.substring(0, 2), treeSha.substring(2));
    if (!Files.exists(objectPath)) {
      System.err.println("Error: Tree object not found for SHA: " + treeSha);
      System.exit(1);
    }

    try (InputStream fileStream = Files.newInputStream(objectPath);
        InflaterInputStream inflaterStream = new InflaterInputStream(fileStream);
        ByteArrayOutputStream decompressedDataStream = new ByteArrayOutputStream()) {

      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inflaterStream.read(buffer)) != -1)
        decompressedDataStream.write(buffer, 0, bytesRead);

      byte[] decompressedBytes = decompressedDataStream.toByteArray();
      int index = 0;

      // Skip header
      while (decompressedBytes[index++] != 0)
        ;

      List<String> names = new ArrayList<>();

      while (index < decompressedBytes.length) {
        // Read mode (e.g., "100644" or "40000") and filename
        int nameStart = index;
        while (decompressedBytes[index] != 0) {
          index++;
        }
        String entry = new String(decompressedBytes, nameStart, index - nameStart, StandardCharsets.UTF_8);
        String[] parts = entry.split(" ", 2); // "100644 filename.txt"
        String fileName = parts[1];
        names.add(fileName);
        index++; // Skip null byte
        index += 20; // Skip SHA1 (binary)
      }

      Collections.sort(names);
      for (String name : names)
        System.out.println(name);

    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void writeTreeCommand() {
    try {
      String treeSha = writeTree(new File("."));
      System.out.println(treeSha);
    } catch (IOException | NoSuchAlgorithmException e) {
      System.err.println("Error writing tree: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void commitTree(String[] args) {
    try {
      String treeSha = args[1];
      String parentSha = null;
      String message = null;
      for (int i = 2; i < args.length; i++) {
        if (args[i].equals("-p"))
          parentSha = args[++i];
        else if (args[i].equals("-m"))
          message = args[++i];
      }
      String author = "Author Name <author@example.com>";
      long timestamp = Instant.now().getEpochSecond();
      String commitContent = "tree " + treeSha + "\n" +
          (parentSha != null ? "parent " + parentSha + "\n" : "") +
          "author " + author + " " + timestamp + " +0000\n" +
          "committer " + author + " " + timestamp + " +0000\n\n" +
          message + "\n";
      byte[] contentBytes = commitContent.getBytes(StandardCharsets.UTF_8);
      String header = "commit " + contentBytes.length + "\0";
      byte[] fullBytes = concatBytes(header.getBytes(StandardCharsets.UTF_8), contentBytes);
      String sha = sha1Hex(fullBytes);
      writeObject(sha, fullBytes);
      System.out.println(sha);
    } catch (Exception e) {
      System.err.println("Error creating commit: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void cloneRepo(String[] args) {
    if (args.length != 3) {
      System.err.println("Usage: git clone <repo_url> <directory>");
      System.exit(1);
    }
    String repoUrl = args[1];
    String targetDir = args[2];
    try {
      File repoDir = new File(targetDir);
      Git.cloneRepository().setURI(repoUrl).setDirectory(repoDir).call();
      System.out.println("Cloned into '" + targetDir + "'");
    } catch (Exception e) {
      System.err.println("Error during cloning: " + e.getMessage());
      System.exit(1);
    }
  }

  private static String writeTree(File dir) throws IOException, NoSuchAlgorithmException {
    List<byte[]> entries = new ArrayList<>();
    File[] files = dir.listFiles(file -> !file.getName().equals(".git"));
    if (files == null)
      return null;
    Arrays.sort(files);
    for (File file : files) {
      String mode;
      String sha;
      if (file.isFile()) {
        mode = "100644";
        byte[] content = Files.readAllBytes(file.toPath());
        String header = "blob " + content.length + "\0";
        byte[] fullContent = concatBytes(header.getBytes(StandardCharsets.UTF_8), content);
        sha = sha1Hex(fullContent);
        writeObject(sha, fullContent);
      } else if (file.isDirectory()) {
        mode = "40000";
        sha = writeTree(file);
      } else {
        continue;
      }
      entries.add(createTreeEntry(mode, file.getName(), sha));
    }
    ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
    for (byte[] entry : entries)
      treeContent.write(entry);
    byte[] fullTree = concatBytes(("tree " + treeContent.size() + "\0").getBytes(StandardCharsets.UTF_8),
        treeContent.toByteArray());
    String treeSha = sha1Hex(fullTree);
    writeObject(treeSha, fullTree);
    return treeSha;
  }

  private static byte[] createTreeEntry(String mode, String name, String shaHex) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write((mode + " " + name).getBytes(StandardCharsets.UTF_8));
    out.write(0);
    out.write(hexToBytes(shaHex));
    return out.toByteArray();
  }

  private static void writeObject(String sha, byte[] content) throws IOException {
    String dirName = sha.substring(0, 2);
    String fileName = sha.substring(2);
    Path objectDir = Paths.get(".git", "objects", dirName);
    Files.createDirectories(objectDir);
    Path objectFile = objectDir.resolve(fileName);
    if (Files.exists(objectFile))
      return;
    try (FileOutputStream fos = new FileOutputStream(objectFile.toFile());
        DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
      dos.write(content);
    }
  }

  private static byte[] concatBytes(byte[] a, byte[] b) {
    byte[] result = new byte[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  private static String sha1Hex(byte[] input) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    byte[] hashBytes = digest.digest(input);
    StringBuilder hexString = new StringBuilder();
    for (byte b : hashBytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1)
        hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  private static byte[] hexToBytes(String hex) {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      int index = i * 2;
      bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
    }
    return bytes;
  }
}
