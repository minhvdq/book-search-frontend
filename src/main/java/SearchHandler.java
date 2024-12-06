import cluster.management.ServiceRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.protobuf.InvalidProtocolBufferException;
import model.frontend.FrontendRequest;
import model.frontend.FrontendResponse;
import model.proto.SearchModel;
import networking.OnRequestHandler;
import networking.WebClient;
import networking.WebServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SearchHandler implements OnRequestHandler {
    private ServiceRegistry serviceRegistry;
    private static final String DOCUMENT_LOCATION = "/books";
    private WebServer server;
    private WebClient client;
    private ObjectMapper objectMapper;
    private static final String ENDPOINT = "/document_search";
    public SearchHandler(ServiceRegistry serviceRegistry){
        this.client = new WebClient();
        this.serviceRegistry = serviceRegistry;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    }
    @Override
    public byte[] requestHandle(byte[] data) {
        try {
            FrontendRequest frontendRequest =
                    objectMapper.readValue(data, FrontendRequest.class);
            System.out.println(frontendRequest.getSearchQuery() + " - search query ");
            FrontendResponse frontendResponse = createFrontendResponse(frontendRequest);
            return objectMapper.writeValueAsBytes(frontendResponse);
        } catch (StreamReadException e) {
            throw new RuntimeException(e);
        } catch (DatabindException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

    private FrontendResponse createFrontendResponse(FrontendRequest frontendRequest) {
        System.out.println("creating response");
        SearchModel.Response searchClusterResponse = sendSearchQueryToCluster(frontendRequest.getSearchQuery());
        List<FrontendResponse.SearchResultInfo> filteredResults =
                filterResults(searchClusterResponse,
                        frontendRequest.getMaxNumberOfResults(),
                        frontendRequest.getMinScore());
        return new FrontendResponse(DOCUMENT_LOCATION, filteredResults);
    }

    private List<FrontendResponse.SearchResultInfo> filterResults(SearchModel.Response searchClusterResponse, long maxNumberOfResults, double minScore) {
        double maxScore = getMaxScore(searchClusterResponse);
        List<FrontendResponse.SearchResultInfo> filteredResults = new ArrayList<>();

        for( int i = 0; i < searchClusterResponse.getDocumentReleventCount() && i < maxNumberOfResults; i ++){
            int normalizedScore = normalizeScore( searchClusterResponse.getDocumentRelevent(i).getScore(), maxScore);
            if( normalizedScore < minScore ){
                break;
            }
            String documentName = searchClusterResponse.getDocumentRelevent(i).getDocumentName();
            String title = getTitle(documentName);
            String extension = getExtension(documentName);

            FrontendResponse.SearchResultInfo searchResultInfo = new FrontendResponse.SearchResultInfo(title, extension, normalizedScore);
            filteredResults.add(searchResultInfo);
        }
        return filteredResults;

    }

    private String getExtension(String documentName) {
        String[] s = documentName.split("\\.");
        if(s.length == 2){
            return s[1];
        }
        return "";
    }

    private String getTitle(String documentName) {
        return documentName.split("\\.")[0];
    }

    private int normalizeScore(double score, double maxScore) {
        return (int) (score * (100/maxScore));
    }

    private double getMaxScore(SearchModel.Response searchClusterResponse) {
        if(searchClusterResponse.getDocumentReleventCount() == 0){
            return 0;
        }
        return searchClusterResponse.getDocumentReleventList()
                .stream()
                .map(document -> document.getScore())
                .max(Double::compare)
                .get();
    }


    private SearchModel.Response sendSearchQueryToCluster(String searchQuery ){
        System.out.println("sending request");
        String coordianatorAddress = serviceRegistry.getRandomAddress();
        System.out.println("got Address: " + coordianatorAddress);
        SearchModel.Request request = SearchModel.Request.newBuilder().setSearchQuery(searchQuery).build();
        try{
            if( coordianatorAddress == null ){
                System.out.println("No search available!");
                return SearchModel.Response.getDefaultInstance();
            }
            byte[] future = client.sendTask(coordianatorAddress, request.toByteArray()).join();
            return SearchModel.Response.parseFrom(future);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
