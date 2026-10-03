package io.github.codingkody99.einkaufsliste.data

/** Serves canned pages so the import flow can be tested without a network. */
class FakeRecipeFetcher(
    private var result: FetchResult = FetchResult.Failure("nicht gesetzt"),
) : RecipeFetcher {

    val requestedUrls = mutableListOf<String>()

    fun returns(html: String) {
        result = FetchResult.Success(html)
    }

    fun fails(reason: String) {
        result = FetchResult.Failure(reason)
    }

    override suspend fun fetch(url: String): FetchResult {
        requestedUrls += url
        return result
    }
}
