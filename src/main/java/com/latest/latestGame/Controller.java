package com.latest.latestGame;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@RestController
public class Controller {


    class GameData{
        public String url;
        public String title;
        public String error;

        public GameData(String title, String url){
            this.title = title;
            this.url = url;

        }
        public GameData(){

        }


    }
    class GamesData{
        public List<GameData> games;
        public String error;
        public GamesData(){
            games = new ArrayList<>();
        }

    }

    private JSONArray getElements(JsonNode node){

        JSONObject object = node.getObject();
        JSONObject data = object.getJSONObject("data");
        JSONObject catalog = data.getJSONObject("Catalog");
        JSONObject searchStore = catalog.getJSONObject("searchStore");
        return searchStore.getJSONArray("elements");

    }

    private Properties loadProps() throws IOException {
        InputStream is = EpicLatestFreeGameApplication.class.getResourceAsStream("/application.properties");
        Properties properties = new Properties();
        properties.load(is);
        return properties;
    }

    @GetMapping("/latest-promos")
    public ResponseEntity<GamesData> getBothFreeGames(){
        GamesData gameData = null;
        try {
            Properties properties = loadProps();
            HttpResponse<JsonNode> response = Unirest.get(properties.getProperty("epic_url")).asJson();
            if(!response.getStatusText().equals("OK"))
                throw new UnirestException("Invalid status code");

            JsonNode json = response.getBody();
            JSONArray elements = getElements(json);
            gameData = new GamesData();

            List<GameData> list = gameData.games;
            for(Object obj: elements){
                JSONObject element = (JSONObject) obj;
                Object promotions = element.get("promotions");


                if(promotions instanceof JSONObject){

                    GameData gd = new GameData(element.getString("title"), properties.getProperty("epic_store_url").concat(element.getString("productSlug")));
                    list.add(gd);




                }

            }

            return new ResponseEntity<>(gameData, HttpStatus.OK);

        } catch (Exception e) {
            gameData = new GamesData();
            gameData.error = e.getMessage();
            return new ResponseEntity<>(gameData, HttpStatus.EXPECTATION_FAILED);
        }

    }



    @GetMapping("/latest-promo")
    public ResponseEntity<GameData> getFreeGame(){
        GameData gameData = null;
        try {
            Properties properties = loadProps();
            HttpResponse<JsonNode> response = Unirest.get(properties.getProperty("epic_url")).asJson();
            if(!response.getStatusText().equals("OK"))
                throw new UnirestException("Invalid status code");

            JsonNode json = response.getBody();
            JSONArray elements = getElements(json);

            gameData = new GameData();
            for(Object obj: elements){
                JSONObject element = (JSONObject) obj;
                Object promotions = element.get("promotions");


                if(promotions instanceof JSONObject){
                    JSONArray offers = ((JSONObject) promotions).getJSONArray("promotionalOffers");
                    if(offers.length() != 0){

                        gameData.title = element.getString("title");
                        gameData.url = properties.getProperty("epic_store_url").concat(element.getString("productSlug"));
                        return new ResponseEntity<>(gameData, HttpStatus.OK);
                    }


                }

            }

            return new ResponseEntity<>(gameData, HttpStatus.OK);

        } catch (Exception e) {
            gameData = new GameData();
            gameData.error = e.getMessage();
            return new ResponseEntity<>(gameData, HttpStatus.EXPECTATION_FAILED);
        }

    }

    @GetMapping("/free-to-plays")
    public ResponseEntity<GamesData> getFreetoPlay(){
        GamesData gameData = null;
        try{
            Properties props = loadProps();
            Document document = Jsoup.connect(props.getProperty("epic_free_to_play_url")).get();
            Elements elements = document.getElementsByClass("css-1jx3eyg");
            gameData = new GamesData();
            List<GameData> list = gameData.games;
            for(Element element: elements){
                String label = element.attr("aria-label");
                String href = element.attr("href").substring(15);
                String url = props.getProperty("epic_store_url").concat(href);
                list.add(new GameData(label, url));
            }
            return new ResponseEntity<>(gameData, HttpStatus.OK);

        }catch(Exception e){
            gameData = new GamesData();
            return new ResponseEntity<>(gameData, HttpStatus.EXPECTATION_FAILED);

        }
    }
}
