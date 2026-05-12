import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

public class FileSplitter {                                                            // pravi novu kolekciju od najmanje 400 fajlova
	public static void main(String[] args) {
		try {
			SplitStats stats = new FileSplitter().splitOriginalFiles();
			System.out.println("Podela zavrsena.");
			System.out.println("Originalnih fajlova: " + stats.originalFiles());
			System.out.println("Kreiranih delova: " + stats.createdFiles());
			System.out.println("Izlazni folder: " + Path.of(BaseConfig.DIREKTORIJUM_DELOVI_PODACI).toAbsolutePath());
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public SplitStats splitOriginalFiles() throws IOException {                         // javna metoda koju koristi i Main
		Path inputDirectory = Path.of(BaseConfig.DIREKTORIJUM_ORIGINALNI_PODACI);
		Path outputDirectory = Path.of(BaseConfig.DIREKTORIJUM_DELOVI_PODACI);

		Files.createDirectories(outputDirectory);
		deleteOldTextFiles(outputDirectory);                                             // cisti stare delove pre novog pokretanja

		List<Path> originalFiles = BaseConfig.listTextFiles(inputDirectory);
		if (originalFiles.size() < 4) {                                                  // zadatak trazi najmanje 4 fajla
			throw new IOException("Potrebna su najmanje 4 originalna .txt fajla u folderu "
					+ inputDirectory.toAbsolutePath());
		}

		int createdFiles = 0;
		for (Path originalFile : originalFiles) {
			createdFiles += splitOneFile(originalFile, outputDirectory);                 // svaka knjiga daje 100 delova
		}

		return new SplitStats(originalFiles.size(), createdFiles);
	}

	private int splitOneFile(Path originalFile, Path outputDirectory) throws IOException { // deli jedan fajl na 100 delova
		String text = Files.readString(originalFile, StandardCharsets.UTF_8);
		String baseName = BaseConfig.removeTxtExtension(originalFile.getFileName().toString());
		int createdFiles = 0;

		for (int index = 0; index < BaseConfig.BROJ_DELOVA_PO_FAJLU; index++) {          // tacno 100 iteracija po knjizi
			int start = index * text.length() / BaseConfig.BROJ_DELOVA_PO_FAJLU;
			int end = (index + 1) * text.length() / BaseConfig.BROJ_DELOVA_PO_FAJLU;
			String part = text.substring(start, end);
			String partName = String.format("%s_deo_%03d.txt", baseName, index + 1);     // ime dela sa rednim brojem 001..100
			Path partPath = outputDirectory.resolve(partName);

			Files.writeString(partPath, part, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			createdFiles++;
		}

		return createdFiles;
	}

	private void deleteOldTextFiles(Path directory) throws IOException {                 // uklanja stare podeljene txt fajlove
		try (Stream<Path> stream = Files.list(directory)) {
			stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".txt"))
					.forEach(FileSplitter::deleteFile);
		} catch (UncheckedIOException exception) {
			throw exception.getCause();
		}
	}

	private static void deleteFile(Path path) {
		try {
			Files.delete(path);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public record SplitStats(int originalFiles, int createdFiles) {
	}
}
