package modules;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class WebServer {
    private static List<Mechanic> mechanics = new ArrayList<>();
    private static List<Client> clients = new ArrayList<>();
    private static List<Car> cars = new ArrayList<>();
    private static List<ServiceVisit> visits = new ArrayList<>();
    private static Inventory inventory = new Inventory();
    private static final Map<String, String> sessions = new HashMap<>();

    public static void setMechanics(List<Mechanic> mechanicList) {
        mechanics = (mechanicList != null) ? mechanicList : new ArrayList<>();
    }

    public static void setData(List<Client> c, List<Car> ca, List<ServiceVisit> v, Inventory inv) {
        clients = (c != null) ? c : new ArrayList<>();
        cars = (ca != null) ? ca : new ArrayList<>();
        visits = (v != null) ? v : new ArrayList<>();
        inventory = (inv != null) ? inv : new Inventory();
    }


    private static boolean isAuthenticated(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        List<String> cookies = headers.get("Cookie");
        if (cookies != null) {
            for (String cookieHeader : cookies) {
                String[] cookieArray = cookieHeader.split(";");
                for (String cookie : cookieArray) {
                    String[] pair = cookie.trim().split("=");
                    if (pair.length == 2 && pair[0].equals("SESSIONID")) {
                        String sessionId = pair[1];
                        return sessions.containsKey(sessionId);
                    }
                }
            }
        }
        return false;
    }

    public static void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/home", new StaticFileHandler("public/home.html"));
        server.createContext("/cars", new CarsHandler());
        server.createContext("/", new StaticFileHandler("public/login.html"));
        server.createContext("/login", new LoginHandler());

        server.createContext("/logout", exchange -> {
            Headers headers = exchange.getRequestHeaders();
            List<String> cookies = headers.get("Cookie");
            if (cookies != null) {
                for (String cookieHeader : cookies) {
                    String[] cookieArray = cookieHeader.split(";");
                    for (String cookie : cookieArray) {
                        String[] pair = cookie.trim().split("=");
                        if (pair.length == 2 && pair[0].equals("SESSIONID")) {
                            String sessionId = pair[1];
                            sessions.remove(sessionId);
                        }
                    }
                }
            }
            exchange.getResponseHeaders().add("Set-Cookie", "SESSIONID=deleted; Path=/; Max-Age=0");
            exchange.getResponseHeaders().set("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
        });

        server.createContext("/orders", exchange -> {
            if (!isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                try {
                    String template = Files.readString(Paths.get("public/new_order_form.html"), StandardCharsets.UTF_8);

                    StringBuilder partOptions = new StringBuilder();
                    for (Part part : inventory.getAllParts()) {
                        partOptions.append("<option value='")
                                .append(part.getName()).append("'>")
                                .append(part.getName()).append(" – ")
                                .append(part.getManufacturer()).append(" (")
                                .append(part.getCategory()).append(")")
                                .append("</option>");
                    }

                    String finalHtml = template.replace("<!-- OPCJE_CZESCI -->", partOptions.toString());


                    byte[] responseBytes = finalHtml.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    exchange.getResponseBody().write(responseBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
                exchange.close();

            } else if ("POST".equalsIgnoreCase(method)) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String formData = br.lines().collect(Collectors.joining());
                    Map<String, List<String>> params = parseMultiValueFormData(formData);

                    List<String> selectedParts = params.get("parts");

                    if (selectedParts != null && !selectedParts.isEmpty()) {
                        StringBuilder notificationMsg = new StringBuilder("Zamówiono części: ");

                        for (String encodedName : selectedParts) {
                            String partName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
                            inventory.getAllParts().stream()
                                    .filter(p -> p.getName().equals(partName))
                                    .findFirst()
                                    .ifPresent(p -> {
                                        p.increaseQuantity(10);
                                        notificationMsg.append(partName).append(", ");
                                    });
                        }

                        if (notificationMsg.toString().endsWith(", ")) {
                            notificationMsg.setLength(notificationMsg.length() - 2);
                        }

                        for (Mechanic mech : mechanics) {
                            NotificationLog log = new NotificationLog(
                                    mech,
                                    notificationMsg.toString(),
                                    NotificationService.NotificationType.EMAIL
                            );
                            mech.addNotification(log);
                        }
                    }

                    exchange.getResponseHeaders().set("Location", "/parts");
                    exchange.sendResponseHeaders(303, -1);
                } catch (Exception e) {
                    e.printStackTrace();
                    String error = "<html><body><h2>Błąd serwera</h2><p>" + e.getMessage() + "</p></body></html>";
                    byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, errorBytes.length);
                    exchange.getResponseBody().write(errorBytes);
                }
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/parts/new", exchange -> {
            if (!isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                byte[] response = Files.readAllBytes(Paths.get("public/new_part_form.html"));
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();

            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {

                    String formData = reader.lines().collect(Collectors.joining());
                    Map<String, String> params = parseFormData(formData);

                    String name = params.getOrDefault("name", "");
                    String manufacturer = params.getOrDefault("manufacturer", "");
                    double price = Double.parseDouble(params.getOrDefault("price", "0"));
                    int quantity = Integer.parseInt(params.getOrDefault("quantity", "0"));
                    String categoryStr = params.getOrDefault("category", "OTHER");
                    int warranty = Integer.parseInt(params.getOrDefault("warranty", "0"));

                    Part.PartCategory category = Part.PartCategory.valueOf(categoryStr.toUpperCase());

                    Part newPart = new Part(name, manufacturer, price, quantity, category, warranty);
                    inventory.addPart(newPart);

                    for (Mechanic mechanic : mechanics) {
                        NotificationLog log = new NotificationLog(
                                mechanic,
                                "Dodano nową część: " + name + " (" + manufacturer + ")",
                                NotificationService.NotificationType.EMAIL
                        );
                        mechanic.addNotification(log);
                    }

                    exchange.getResponseHeaders().set("Location", "/parts");
                    exchange.sendResponseHeaders(303, -1);
                } catch (Exception e) {
                    e.printStackTrace();
                    String response = "<html><body><h2>Błąd serwera</h2><p>" + e.getMessage() + "</p></body></html>";
                    byte[] errorBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(500, errorBytes.length);
                    exchange.getResponseBody().write(errorBytes);
                } finally {
                    exchange.getResponseBody().close();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/cars/new", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String template = Files.readString(Paths.get("public/new_car_form.html"), StandardCharsets.UTF_8);
                StringBuilder options = new StringBuilder();
                for (Client client : clients) {
                    options.append("<option value='").append(client.getEmail()).append("'>")
                            .append(client.getFirstName()).append(" ").append(client.getLastName())
                            .append(" – ").append(client.getEmail()).append("</option>");
                }
                String filledHtml = template.replace("<!-- opcje klientów do uzupełnienia dynamicznie -->", options.toString());

                byte[] response = filledHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();

            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder formDataBuilder = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        formDataBuilder.append(line);
                    }
                    String formData = formDataBuilder.toString();
                    Map<String, String> params = parseFormData(formData);

                    String email = URLDecoder.decode(params.get("ownerEmail"), StandardCharsets.UTF_8);
                    String brand = URLDecoder.decode(params.get("brand"), StandardCharsets.UTF_8);
                    String model = URLDecoder.decode(params.get("model"), StandardCharsets.UTF_8);
                    String vin = URLDecoder.decode(params.get("vin"), StandardCharsets.UTF_8);
                    int year = Integer.parseInt(params.get("year"));

                    Client owner = clients.stream()
                            .filter(c -> c.getEmail().equals(email))
                            .findFirst().orElse(null);

                    if (owner != null) {
                        Car newCar = new Car(vin, brand, model, year, owner);
                        cars.add(newCar);
                        owner.addCar(newCar);

                        for (Mechanic mechanic : mechanics) {
                            NotificationLog log = new NotificationLog(
                                    mechanic,
                                    "Dodano nowy samochód: " + brand + " " + model + " (VIN: " + vin + ") dla klienta " +
                                            owner.getFirstName() + " " + owner.getLastName(),
                                    NotificationService.NotificationType.EMAIL
                            );
                            mechanic.addNotification(log);
                        }

                        exchange.getResponseHeaders().set("Location", "/clients");
                        exchange.sendResponseHeaders(303, -1);
                        exchange.close();
                    } else {
                        String error = "Nie znaleziono klienta o adresie: " + email;
                        byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(400, bytes.length);
                        exchange.getResponseBody().write(bytes);
                        exchange.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    String error = "<html><body><h2>Błąd serwera</h2><p>" + e.getMessage() + "</p></body></html>";
                    byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, errorBytes.length);
                    exchange.getResponseBody().write(errorBytes);
                    exchange.getResponseBody().close();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/notifications", exchange -> {
            if (!isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            String sessionId = Arrays.stream(exchange.getRequestHeaders().getFirst("Cookie").split(";"))
                    .map(String::trim)
                    .filter(cookie -> cookie.startsWith("SESSIONID="))
                    .map(cookie -> cookie.substring("SESSIONID=".length()))
                    .findFirst()
                    .orElse(null);

            String email = sessions.get(sessionId);
            Mechanic loggedMechanic = mechanics.stream()
                    .filter(m -> m.getEmail().equals(email))
                    .findFirst().orElse(null);

            if (loggedMechanic == null) {
                exchange.sendResponseHeaders(403, -1);
                exchange.getResponseBody().close();
                return;
            }

            try {
                String template = Files.readString(Paths.get("public/notifications.html"), StandardCharsets.UTF_8);
                StringBuilder notificationsHtml = new StringBuilder();

                for (NotificationLog log : loggedMechanic.getNotifications()) {
                    notificationsHtml.append("<div class='notification'>")
                            .append("<p><strong>Typ:</strong> ").append(log.getType()).append("</p>")
                            .append("<p><strong>Treść:</strong> ").append(log.getMessage()).append("</p>")
                            .append("<p><strong>Data:</strong> ").append(log.getTimestamp()).append("</p>")
                            .append("</div>");
                }

                String filledHtml = template.replace("<!-- POWIADOMIENIA -->", notificationsHtml.toString());

                byte[] response = filledHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                String error = "<html><body><h2>Błąd serwera</h2><p>" + e.getMessage() + "</p></body></html>";
                byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, errorBytes.length);
                exchange.getResponseBody().write(errorBytes);
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/clients/new", exchange -> {
            try {
                System.out.println("\n--- Żądanie do /clients/new ---");
                System.out.println("Metoda: " + exchange.getRequestMethod());

                if ("GET".equals(exchange.getRequestMethod())) {
                    byte[] response = Files.readAllBytes(Paths.get("public/new_client_form.html"));
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder formDataBuilder = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        formDataBuilder.append(line);
                    }
                    String formData = formDataBuilder.toString();
                    System.out.println("Formularz: " + formData);

                    Map<String, String> params = parseFormData(formData);

                    String name = params.getOrDefault("firstName", "").trim();
                    String surname = params.getOrDefault("lastName", "").trim();
                    String email = params.getOrDefault("email", "").trim();
                    String phone = params.getOrDefault("phone", "").trim();

                    if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                        String error = "Nieprawidłowe dane wejściowe.";
                        exchange.sendResponseHeaders(400, error.length());
                        exchange.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
                        exchange.getResponseBody().close();
                        return;
                    }

                    Client newClient = new Client(name, surname, email, phone);
                    clients.add(newClient);
                    System.out.println("Dodano klienta: " + name + " " + surname + " (" + email + ", " + phone + ")");

                    for (Mechanic mechanic : mechanics) {
                        NotificationLog log = new NotificationLog(mechanic,
                                "Dodano nowego klienta: " + name + " " + surname,
                                NotificationService.NotificationType.EMAIL);
                        mechanic.addNotification(log);
                    }

                    exchange.getResponseHeaders().set("Location", "/clients");
                    exchange.sendResponseHeaders(303, -1);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
                exchange.getResponseBody().close();
            } catch (Exception e) {
                e.printStackTrace();
                String response = "Błąd serwera: " + e.getMessage();
                byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, respBytes.length);
                exchange.getResponseBody().write(respBytes);
                exchange.getResponseBody().close();
            }
        });

        server.createContext("/clients", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String template = Files.readString(Paths.get("public/clients.html"), StandardCharsets.UTF_8);
                StringBuilder dynamicHtml = new StringBuilder();

                for (Client client : clients) {
                    dynamicHtml.append("<div class='client'>")
                            .append("<h2>").append(client.getFirstName()).append(" ").append(client.getLastName()).append("</h2>")
                            .append("<p>Email: ").append(client.getEmail())
                            .append("<br>Telefon: ").append(client.getPhoneNumber()).append("</p>");

                    List<Car> clientCars = client.getCars();

                    if (clientCars.isEmpty()) {
                        dynamicHtml.append("<p><i>Brak przypisanych samochodów.</i></p>");
                    } else {
                        dynamicHtml.append("<div class='cars'>");

                        for (Car car : clientCars) {
                            dynamicHtml.append("<p><b>")
                                    .append(car.getBrand()).append(" ").append(car.getModel())
                                    .append(" (VIN: ").append(car.getVin()).append(")</b></p>");

                            List<ServiceVisit> carVisits = visits.stream()
                                    .filter(v -> v.getCar().equals(car))
                                    .collect(Collectors.toList());

                            if (carVisits.isEmpty()) {
                                dynamicHtml.append("<p><i>Brak wizyt serwisowych.</i></p>");
                            } else {
                                dynamicHtml.append("<ul>");
                                for (ServiceVisit visit : carVisits) {
                                    dynamicHtml.append("<li>")
                                            .append("Data: ").append(visit.getVisitDate())
                                            .append(" — Opis: ").append(visit.getDescription())
                                            .append(" — Koszt: ").append(visit.getCost()).append(" zł");

                                    List<Part> usedParts = visit.getParts();
                                    if (usedParts != null && !usedParts.isEmpty()) {
                                        dynamicHtml.append("<br><i>Części użyte:</i><ul>");
                                        for (Part part : usedParts) {
                                            dynamicHtml.append("<li>")
                                                    .append(part.getName())
                                                    .append(" (").append(part.getManufacturer()).append(")")
                                                    .append("</li>");
                                        }
                                        dynamicHtml.append("</ul>");
                                    }

                                    dynamicHtml.append("</li>");
                                }
                                dynamicHtml.append("</ul>");
                            }
                        }

                        dynamicHtml.append("</div>");
                    }

                    dynamicHtml.append("</div>");
                }

                String finalHtml = template.replace(
                        "<!-- Sekcje klientów z samochodami i linkami do wizyt wstawiane dynamicznie przez backend -->",
                        dynamicHtml.toString()
                );

                byte[] response = finalHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        });


        server.createContext("/visits/new", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String template = Files.readString(Paths.get("public/new_visit_form.html"), StandardCharsets.UTF_8);

                StringBuilder clientOptions = new StringBuilder();
                Map<String, List<Car>> clientCarsMap = new HashMap<>();

                for (Client c : clients) {
                    clientOptions.append("<option value='").append(c.getEmail()).append("'>")
                            .append(c.getFirstName()).append(" ").append(c.getLastName())
                            .append(" – ").append(c.getEmail())
                            .append("</option>");
                    clientCarsMap.put(c.getEmail(), c.getCars());
                }

                StringBuilder carOptions = new StringBuilder();
                for (Map.Entry<String, List<Car>> entry : clientCarsMap.entrySet()) {
                    for (Car car : entry.getValue()) {
                        carOptions.append("<option value='")
                                .append(car.getVin()).append("' data-owner='")
                                .append(entry.getKey()).append("'>")
                                .append(car.getBrand()).append(" ").append(car.getModel())
                                .append(" (VIN: ").append(car.getVin()).append(") – ")
                                .append(car.getOwner().getFirstName()).append(" ").append(car.getOwner().getLastName())
                                .append("</option>");
                    }
                }

                StringBuilder partOptions = new StringBuilder();
                for (Part part : inventory.getAllParts()) {
                    partOptions.append("<option value='").append(part.getName()).append("'>")
                            .append(part.getName()).append(" – ").append(part.getManufacturer())
                            .append("(Cena: ").append(part.getPrice()).append(" zł, Ilość: ")
                            .append(part.getQuantity()).append(")")
                            .append("</option>");
                }

                String filledHtml = template
                        .replace("<!-- OPCJE_KLIENTOW -->", clientOptions.toString())
                        .replace("<!-- OPCJE_SAMOCHODOW -->", carOptions.toString())
                        .replace("<!-- OPCJE_CZESCI -->", partOptions.toString())
                        .replace("</body>", "<script>\n" +
                                "const clientSelect = document.querySelector('select[name=\"client\"]');\n" +
                                "const carSelect = document.querySelector('select[name=\"car\"]');\n" +
                                "const allCarOptions = Array.from(carSelect.options);\n" +
                                "\n" +
                                "function filterCarsByClient(email) {\n" +
                                "  carSelect.innerHTML = '<option value=\"\">-- Wybierz samochód --</option>';\n" +
                                "  allCarOptions.forEach(opt => {\n" +
                                "    if (opt.dataset.owner === email) {\n" +
                                "      carSelect.appendChild(opt);\n" +
                                "    }\n" +
                                "  });\n" +
                                "}\n" +
                                "\n" +
                                "clientSelect.addEventListener('change', () => {\n" +
                                "  filterCarsByClient(clientSelect.value);\n" +
                                "});\n" +
                                "</script></body>");

                byte[] bytes = filledHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();

            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder formDataBuilder = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        formDataBuilder.append(line);
                    }
                    String formData = formDataBuilder.toString();
                    System.out.println("Formularz wizyty: " + formData);

                    Map<String, String> params = parseFormData(formData);

                    String clientEmail = URLDecoder.decode(params.getOrDefault("client", ""), StandardCharsets.UTF_8);
                    String carVin = URLDecoder.decode(params.getOrDefault("car", ""), StandardCharsets.UTF_8);
                    String dateStr = URLDecoder.decode(params.getOrDefault("date", ""), StandardCharsets.UTF_8);
                    String description = URLDecoder.decode(params.getOrDefault("description", ""), StandardCharsets.UTF_8);
                    String costStr = URLDecoder.decode(params.getOrDefault("cost", "0"), StandardCharsets.UTF_8);

                    Client client = clients.stream()
                            .filter(c -> c.getEmail().equals(clientEmail))
                            .findFirst()
                            .orElse(null);

                    Car car = (client != null) ? client.getCars().stream()
                            .filter(c -> c.getVin().equals(carVin))
                            .findFirst().orElse(null) : null;

                    List<Part> selectedParts = new ArrayList<>();
                    if (formData.contains("parts=")) {
                        List<String> partNames = Arrays.stream(formData.split("&"))
                                .filter(pair -> pair.startsWith("parts="))
                                .map(pair -> URLDecoder.decode(pair.substring(6), StandardCharsets.UTF_8))
                                .collect(Collectors.toList());

                        for (String partName : partNames) {
                            inventory.getAllParts().stream()
                                    .filter(p -> p.getName().equals(partName))
                                    .findFirst()
                                    .ifPresent(part -> {
                                        selectedParts.add(part);
                                        part.usePart(1);
                                    });
                        }
                    }


                    double cost = Double.parseDouble(costStr);
                    Date date = java.sql.Date.valueOf(dateStr);

                    if (client != null && car != null) {
                        ServiceVisit visit = new ServiceVisit(date, description, cost, car, null, selectedParts);
                        visits.add(visit);
                        notifyMechanicsAboutNewVisit(visit);
                        System.out.println("Zapisano wizytę: " + description);
                        exchange.getResponseHeaders().set("Location", "/clients");
                        exchange.sendResponseHeaders(303, -1);
                        exchange.close();
                    } else {
                        String error = "<html><body><h2>Błąd: Nie znaleziono klienta lub samochodu</h2></body></html>";
                        byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(400, errorBytes.length);
                        exchange.getResponseBody().write(errorBytes);
                        exchange.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    String error = "<html><body><h2>Błąd serwera</h2><p>" + e.getMessage() + "</p></body></html>";
                    byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, errorBytes.length);
                    exchange.getResponseBody().write(errorBytes);
                    exchange.close();
                }

            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        });



        server.createContext("/parts", exchange -> {
            if (!isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String template = Files.readString(Paths.get("public/parts.html"), StandardCharsets.UTF_8);
                StringBuilder rows = new StringBuilder();

                for (Part part : inventory.getAllParts()) {
                    rows.append("<tr>")
                            .append("<td>").append(part.getId()).append("</td>")
                            .append("<td>").append(part.getName()).append("</td>")
                            .append("<td>").append(part.getManufacturer()).append("</td>")
                            .append("<td>").append(part.getPrice()).append(" zł</td>")
                            .append("<td>").append(part.getQuantity()).append("</td>")
                            .append("<td>").append(part.getCategory() != null ? part.getCategory().name() : "–").append("</td>")
                            .append("<td>").append(part.getWarrantyMonths()).append(" mies.").append("</td>")
                            .append("</tr>");
                }

                String filledHtml = template.replace(
                        "<!-- Wiersze części zostaną wstawione dynamicznie przez Java -->",
                        rows.toString()
                );

                byte[] response = filledHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        });



        server.setExecutor(null);
        server.start();
        System.out.println("Serwer działa na http://localhost:" + port);
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String formData = br.readLine();

                Map<String, String> params = new HashMap<>();
                if (formData != null) {
                    String[] pairs = formData.split("&");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                        }
                    }
                }

                String email = params.getOrDefault("username", "");
                String password = params.getOrDefault("password", "");

                boolean authenticated = mechanics.stream().anyMatch(m ->
                        m.getEmail().equals(email) && m.getPhoneNumber().equals(password)
                );

                if (authenticated) {
                    String sessionId = UUID.randomUUID().toString();
                    sessions.put(sessionId, email);
                    exchange.getResponseHeaders().add("Set-Cookie", "SESSIONID=" + sessionId + "; Path=/; HttpOnly");
                    exchange.getResponseHeaders().add("Location", "/home");
                    exchange.sendResponseHeaders(302, -1);
                } else {
                    String response = """
                    <html><head><meta charset='UTF-8'><title>Błąd logowania</title></head>
                    <body style='text-align:center;'>
                        <h2>Błędny login lub hasło</h2>
                        <a href='/login'>Wróć do logowania</a>
                    </body></html>
                    """;
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                }
            } else {
                new StaticFileHandler("public/login.html").handle(exchange);
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        private final String filePath;
        public StaticFileHandler(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File(filePath);
            if (!file.exists()) {
                String notFound = "<h1>404 - Nie znaleziono pliku</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                OutputStream os = exchange.getResponseBody();
                os.write(notFound.getBytes());
                os.close();
                return;
            }
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class CarsHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            StringBuilder html = new StringBuilder("""
        <html><head><meta charset='UTF-8'></head><body style='text-align:center;'>
        <h2>Lista samochodów</h2><ul>
        """);

            for (Car car : cars) {
                html.append("<li>")
                        .append(car.getBrand()).append(" ")
                        .append(car.getModel()).append(" – VIN: ")
                        .append(car.getVin()).append(" – Właściciel: ")
                        .append(car.getOwner().getFirstName()).append(" ")
                        .append(car.getOwner().getLastName())
                        .append("</li>");
            }

            html.append("</ul><a href='/home'><button>Powrót</button></a></body></html>");

            byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class PartsHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            String template = Files.readString(Paths.get("public/parts.html"), StandardCharsets.UTF_8);
            StringBuilder rows = new StringBuilder();

            for (Part part : inventory.getAllParts()) {
                rows.append("<tr>")
                        .append("<td>").append(part.getId()).append("</td>")
                        .append("<td>").append(part.getName()).append("</td>")
                        .append("<td>").append(part.getManufacturer()).append("</td>")
                        .append("<td>").append(part.getPrice()).append(" zł</td>")
                        .append("<td>").append(part.getQuantity()).append("</td>")
                        .append("<td>").append(part.getCategory()).append("</td>")
                        .append("</tr>");
            }

            String finalHtml = template.replace(
                    "<!-- Wiersze części zostaną wstawione dynamicznie przez Java -->",
                    rows.toString()
            );

            byte[] response = finalHtml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    public static void notifyMechanicsAboutNewClient(Client client) {
        for (Mechanic mechanic : mechanics) {
            NotificationLog log = new NotificationLog(mechanic,
                    "Dodano nowego klienta: " + client.getFirstName() + " " + client.getLastName(),
                    NotificationService.NotificationType.EMAIL);
            mechanic.addNotification(log);
        }
    }

    public static void notifyMechanicsAboutNewCar(Car car) {
        for (Mechanic mechanic : mechanics) {
            NotificationLog log = new NotificationLog(mechanic,
                    "Dodano nowy samochód: " + car.getBrand() + " " + car.getModel(),
                    NotificationService.NotificationType.EMAIL);
            mechanic.addNotification(log);
        }
    }

    public static void notifyMechanicsAboutNewVisit(ServiceVisit visit) {
        for (Mechanic mechanic : mechanics) {
            NotificationLog log = new NotificationLog(mechanic,
                    "Dodano nową wizytę: " + visit.getDescription(),
                    NotificationService.NotificationType.EMAIL);
            mechanic.addNotification(log);
        }
    }

    public static void useParts(List<Part> parts) {
        for (Part part : parts) {
            part.usePart(1);
        }
    }


    private static Map<String, String> parseFormData(String formData) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }

    private static Map<String, List<String>> parseMultiValueFormData(String formData) {
        Map<String, List<String>> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }
        return map;
    }

}
