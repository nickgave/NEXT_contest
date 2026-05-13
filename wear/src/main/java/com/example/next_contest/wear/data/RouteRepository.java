package com.example.next_contest.wear.data;

import com.example.next_contest.wear.model.NavStep;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RouteRepository {
    public List<NavStep> requestRouteSteps(
            double startLatitude,
            double startLongitude,
            double destinationLatitude,
            double destinationLongitude,
            String apiKey
    ) throws Exception {
        URL url = new URL("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("appKey", apiKey);
            connection.setDoOutput(true);

            JSONObject body = new JSONObject()
                    .put("startX", String.valueOf(startLongitude))
                    .put("startY", String.valueOf(startLatitude))
                    .put("endX", String.valueOf(destinationLongitude))
                    .put("endY", String.valueOf(destinationLatitude))
                    .put("reqCoordType", "WGS84GEO")
                    .put("resCoordType", "WGS84GEO")
                    .put("startName", "watch")
                    .put("endName", "home");

            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(body.toString());
            }

            int responseCode = connection.getResponseCode();
            String response = readStream(responseCode >= 200 && responseCode <= 299
                    ? connection.getInputStream()
                    : connection.getErrorStream());

            if (responseCode < 200 || responseCode > 299) {
                throw new IllegalStateException("Tmap API error " + responseCode + ": " + response);
            }

            if (response.trim().isEmpty()) {
                throw new IllegalStateException("Tmap API 응답이 비어 있습니다.");
            }

            return parseRouteSteps(response);
        } finally {
            connection.disconnect();
        }
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private List<NavStep> parseRouteSteps(String response) throws Exception {
        JSONArray features = new JSONObject(response).optJSONArray("features");
        if (features == null) {
            throw new IllegalStateException("Tmap 응답에 features가 없습니다.");
        }

        List<NavStep> steps = new ArrayList<>();

        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject properties = feature.optJSONObject("properties");
            JSONObject geometry = feature.optJSONObject("geometry");

            if (properties == null || geometry == null) {
                continue;
            }

            String instruction = properties.optString("description");
            JSONArray point = getRepresentativePoint(geometry);

            if (instruction.trim().isEmpty() || point == null || point.length() < 2) {
                continue;
            }

            steps.add(new NavStep(
                    instruction,
                    point.getDouble(1),
                    point.getDouble(0),
                    properties.optInt("distance", 0)
            ));
        }

        return steps;
    }

    private JSONArray getRepresentativePoint(JSONObject geometry) {
        JSONArray coordinates = geometry.optJSONArray("coordinates");
        if (coordinates == null || coordinates.length() == 0) {
            return null;
        }

        String type = geometry.optString("type");
        if ("Point".equals(type)) {
            return coordinates;
        }

        if ("LineString".equals(type)) {
            return coordinates.optJSONArray(coordinates.length() - 1);
        }

        return null;
    }
}
