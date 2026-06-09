import java.nio.file.Path;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;

public class Main {
	public static void main(String[] args) {
		try {
			System.out.println("LAB 2 - Lucene mere slicnosti i rangiranje rezultata");

			Indexer indexer = new Indexer();
			Indexer.IndexStats bm25Stats = indexer.createIndex(
					Path.of(BaseConfig.DIREKTORIJUM_PODACI),
					Path.of(BaseConfig.DIREKTORIJUM_INDEKS_BM25),
					new BM25Similarity(),
					"BM25Similarity");
			Indexer.IndexStats classicStats = indexer.createIndex(
					Path.of(BaseConfig.DIREKTORIJUM_PODACI),
					Path.of(BaseConfig.DIREKTORIJUM_INDEKS_CLASSIC),
					new ClassicSimilarity(),
					"ClassicSimilarity");

			Indexer.printStats(bm25Stats);
			Indexer.printStats(classicStats);

			Searcher.runAllSearches();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
}
