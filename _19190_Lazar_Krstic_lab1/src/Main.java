import java.nio.file.Path;

public class Main {                                                                    
	public static void main(String[] args) {                                             
		try {
			System.out.println("LAB 1 - Lucene indeksiranje i pretraga");

			FileSplitter.SplitStats splitStats = new FileSplitter().splitOriginalFiles();
			System.out.println("Podela originalnih fajlova");
			System.out.println("Originalnih fajlova: " + splitStats.originalFiles());
			System.out.println("Kreiranih delova: " + splitStats.createdFiles());

			Indexer indexer = new Indexer();
			Indexer.IndexStats originalIndexStats = indexer.createIndex(                 // indeksira 4 originalna fajla
					Path.of(BaseConfig.DIREKTORIJUM_ORIGINALNI_PODACI),                  
					Path.of(BaseConfig.DIREKTORIJUM_ORIGINALNI_INDEKS));                 
			Indexer.IndexStats splitIndexStats = indexer.createIndex(                    // indeksira 400 delova
					Path.of(BaseConfig.DIREKTORIJUM_DELOVI_PODACI),                      
					Path.of(BaseConfig.DIREKTORIJUM_DELOVI_INDEKS));                     

			Indexer.printStats(originalIndexStats);                                      
			Indexer.printStats(splitIndexStats);                                         
			Indexer.printComparison(originalIndexStats, splitIndexStats);                

			Searcher.runAllQueries();                                                    // pokrece oba tipa upita nad oba indeksa
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
}
