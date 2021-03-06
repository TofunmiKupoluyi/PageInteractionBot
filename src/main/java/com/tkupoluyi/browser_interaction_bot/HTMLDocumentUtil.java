package com.tkupoluyi.browser_interaction_bot;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.jsoup.*;

import java.util.*;

public class HTMLDocumentUtil {

    private Document doc;
    private Map<String, Integer> globalXpathMap;
    private ChromeDriver driver;
    private LinkedList<Event> eventList;

    HTMLDocumentUtil(ChromeDriver driver) {
        this.doc = Jsoup.parse(driver.getPageSource());
        this.driver = driver;
        this.eventList = new LinkedList<>();
        this.globalXpathMap = new HashMap<>();
        dfs(doc.body(), "/html/body");
    }

    private void dfs(Element root, String xpath) {
        ArrayList<Map> listeners = getEventListenersByXpath(xpath);

        for (Map listener: listeners) {
            Event event = new Event((String) listener.get("type"), xpath);
            this.eventList.add(event);
        }

        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i< root.childrenSize(); i++) {
            Element child = root.child(i);
            String tagName = child.tagName();
            if (!tagName.equals("link") && !tagName.equals("script") && !tagName.equals("style") && !tagName.equals("svg") && !tagName.equals("img")) {
                int currTagIndex = map.getOrDefault(tagName, 0) + 1;
                map.put(tagName, currTagIndex);
                if (child.attributes().hasKey("id")) {
                    String childXpath = "//" + tagName + "[@id='" + child.attributes().get("id") + "']";
                    Integer index = globalXpathMap.getOrDefault(childXpath, 0) + 1;
                    globalXpathMap.put(childXpath, index);
                    dfs(child, "("+childXpath+")"+"["+index+"]");
                } else if (child.attributes().hasKey("class")) {
                    String childXpath = "//" + tagName + "[@class='" + child.attributes().get("class") + "']";
                    Integer index = this.globalXpathMap.getOrDefault(childXpath, 0) + 1;
                    globalXpathMap.put(childXpath, index);
                    dfs(child, "("+childXpath+")"+"["+index+"]");
                } else {
                    dfs(child, xpath + "/" + tagName + "[" + currTagIndex + "]");
                }
            }
        }
    }

    private ArrayList<Map> getEventListenersByXpath(String xpath) {
        Map query = new HashMap()
        {{
            put("expression", "document.evaluate(\""+xpath+"\",document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null).snapshotItem(0)");
        }};

        return getEventListenerFromQuery(xpath, query);
    }

    private ArrayList<Map> getEventListenerFromQuery(String xpath, Map query) {
        try {
            Map result = (Map) driver.executeCdpCommand("Runtime.evaluate", query).get("result");
            String objectId = (String) result.get("objectId");
            ArrayList<Map> listeners = getEventListenersByObjectId(objectId);
            return listeners;
        } catch (WebDriverException ex) {
            System.out.println("Trouble locating xpath, " + xpath);
        }
        return (new ArrayList<Map>());
    }

    private ArrayList<Map> getEventListenersByObjectId(String objectId) {
        Map query = new HashMap()
        {{
            put("objectId", objectId);
        }};
        Map result = driver.executeCdpCommand("DOMDebugger.getEventListeners", query);
        return (ArrayList) result.get("listeners");
    }

    public LinkedList<Event> getEventList() {
        return eventList;
    }
}