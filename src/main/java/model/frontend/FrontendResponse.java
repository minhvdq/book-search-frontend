package model.frontend;

import java.util.Collections;
import java.util.List;

public class FrontendResponse {
    private List<SearchResultInfo> searchResults = Collections.emptyList();
    private String documentLocation = "";

    public FrontendResponse(String documentLocation, List<SearchResultInfo> searchResults){
        this.documentLocation = documentLocation;
        this.searchResults = searchResults;
    }

    public List<SearchResultInfo> getSearchResults() {
        return searchResults;
    }

    public String getDocumentLocation() {
        return documentLocation;
    }

    public static class SearchResultInfo {

        private String title;

        private String extension;
        private int score;

        public SearchResultInfo(String title, String extension, int score) {
            this.title = title;
            this.extension = extension;
            this.score = score;
        }
        public String getExtension() {
            return extension;
        }

        public String getTitle() {
            return title;
        }

        public int getScore() {
            return score;
        }
    }
}
