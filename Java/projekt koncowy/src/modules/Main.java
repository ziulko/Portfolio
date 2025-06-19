package modules;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        List<Mechanic> mechanics = new ArrayList<>();
        List<Client> clients = new ArrayList<>();
        List<Car> cars = new ArrayList<>();
        List<ServiceVisit> visits = new ArrayList<>();
        List<Part> parts = new ArrayList<>();

        //Mechanicy
        Mechanic mech1 = new Mechanic("Jan", "Kowalski", "111-222-333", "jan@warsztat.pl", "silniki");
        Mechanic mech2 = new Mechanic("Anna", "Nowak", "222-333-444", "anna@warsztat.pl", "elektryka");
        mechanics.add(mech1);
        mechanics.add(mech2);

        //Klienci
        Client client1 = new Client("Adam", "Krawczyk", "555-666-777", "adam@klient.pl");
        Client client2 = new Client("Ewa", "Zielińska", "888-999-000", "ewa@klient.pl");
        clients.add(client1);
        clients.add(client2);

        //Samochody
        Car car1 = new Car("1HGBH41JXMN109186", "Toyota", "Corolla", 2015, client1);
        Car car2 = new Car("WVWZZZ1JZXW000001", "Volkswagen", "Golf", 2017, client2);
        client1.addCar(car1);
        client2.addCar(car2);
        cars.add(car1);
        cars.add(car2);

        //Magazyn
        Inventory inventory = new Inventory();

        // Części
        Part part1 = new Part("Filtr oleju", "Bosch", 40.0, 10, Part.PartCategory.MAINTENANCE, 12);
        Part part2 = new Part("Świeca zapłonowa", "NGK", 25.0, 20, Part.PartCategory.ESSENTIAL, 24);
        part1.setInventory(inventory);
        part2.setInventory(inventory);
        inventory.addPart(part1);
        inventory.addPart(part2);
        parts.add(part1);
        parts.add(part2);

        //Operacje na magazynie
        inventory.restockPart("Filtr oleju", 5);
        System.out.println("Wartość magazynu: " + inventory.getTotalInventoryValue() + " PLN");
        List<Part> essentialParts = inventory.findPartsByCategory(Part.PartCategory.ESSENTIAL);
        System.out.println("Części ESSENTIAL: " + essentialParts.size());

        //Wizyty serwisowe
        List<Part> usedParts1 = new ArrayList<>(List.of(part1));
        List<Part> usedParts2 = new ArrayList<>(List.of(part2));
        ServiceVisit visit1 = new ServiceVisit(new Date(), "Wymiana oleju", 150.0, car1, mech1, usedParts1);
        ServiceVisit visit2 = new ServiceVisit(new Date(), "Wymiana świec", 120.0, car2, mech2, usedParts2);
        visit1.setStatus(ServiceVisit.Status.COMPLETED);
        visit2.setStatus(ServiceVisit.Status.COMPLETED);
        visits.add(visit1);
        visits.add(visit2);

        WebServer.setMechanics(mechanics);
        WebServer.setData(clients, cars, visits, inventory);
        WebServer.start(8080);
    }
}
