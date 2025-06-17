package com.pricenotifier.backend;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
public class PriceScraperService {
    public List<String> scrapeEbayTitles(String query) throws IOException {
        String url = "https://www.ebay.com/sch/i.html?_nkw=" + query;
        Document doc = Jsoup.connect(url).get();

        Elements listings = doc.select(".s-item__title");
        List<String> results = new ArrayList<>();

        for (Element item : listings) {
            String title = item.text();
            if (!title.isEmpty() && !title.equals("Shop on eBay")) {
                results.add(title);
            }
        }

        return results;
    }
}
