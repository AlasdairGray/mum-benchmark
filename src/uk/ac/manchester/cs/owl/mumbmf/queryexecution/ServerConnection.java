package uk.ac.manchester.cs.owl.mumbmf.queryexecution;


public interface ServerConnection {

	public void executeQuery(Query query, byte queryType);

    public Object executeSimpleQuery(String queryString);

	public void close();
}

