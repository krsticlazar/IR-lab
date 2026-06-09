public class Main {
	public static void main(String[] args) {
		try {
			System.out.println("LAB 3 - Lucene i Tika indeksiranje dokumenata razlicitih formata");

			Indexer indexer = new Indexer();
			Indexer.printStats(indexer.createTextIndex());
			Indexer.printStats(indexer.createTikaIndex());

			Searcher.runAllQueries();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
}
