package pt.ist.fenixedu.parking.tasks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Base64;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.parking.domain.NewParkingDocument;
import pt.ist.fenixedu.parking.domain.ParkingGroup;
import pt.ist.fenixedu.parking.domain.ParkingParty;
import pt.ist.fenixedu.parking.domain.ParkingPartyHistory;
import pt.ist.fenixedu.parking.domain.ParkingRequest;
import pt.ist.fenixedu.parking.domain.Vehicle;

public class ExportParkingDataToJson extends CustomTask {

    private final String exportTo = "/path/to/file/parking.json";

    @Override
    public void runTask() throws Exception {
        JsonObject allData = new JsonObject();

        // Parking Groups
        JsonArray parkingGroupsData = new JsonArray();
        for (ParkingGroup group : ParkingGroup.getAll()) {
            JsonObject parkingGroupData = new JsonObject();
            parkingGroupData.addProperty("name", group.getGroupName());
            parkingGroupsData.add(parkingGroupData);
        }
        allData.add("parkingGroups", parkingGroupsData);
        
        // Parking Parties
        JsonArray parkingPartiesData = new JsonArray();
        for (ParkingParty party : ParkingParty.getAll()) {
            JsonObject parkingPartyData = new JsonObject();
            if (party.getParty() == null) {
                this.getLogger().warn("Parking Party " + party.getExternalId() + " without associated Party");
                continue;
            }
            if (party.getParty().isPerson() && ((Person) party.getParty()).getUsername() != null) {
                parkingPartyData.addProperty("username", ((Person) party.getParty()).getUsername());
            } else {
                this.getLogger().warn("Parking Party " + party.getExternalId() + " with associated non-Person Party");
            }
            parkingPartyData.addProperty("phoneNr", party.getParty().getDefaultMobilePhoneNumber());
            parkingPartyData.addProperty("email", party.getParty().getDefaultEmailAddressValue());

            // Driver's License
            parkingPartyData.add("driversLicense", fileToJson(party.getDriverLicenseDocument()));

            // Access Card
            Long card = party.getCardNumber();
            if (card == null) {
                this.getLogger().info("Parking Party " + party.getExternalId() + " has no Access Card");
            } else {
                JsonObject cardData = new JsonObject();
                String start = party.getCardStartDate() == null ? null : party.getCardStartDate().toString();
                String end = party.getCardEndDate() == null ? null : party.getCardEndDate().toString();

                cardData.addProperty("number", card);
                cardData.addProperty("start", start);
                cardData.addProperty("end", end);

                parkingPartyData.add("card", cardData);
            }

            // Vehicles
            JsonArray vehiclesData = new JsonArray();
            for (Vehicle vehicle : party.getVehiclesSet()) {
                JsonObject vehicleData = new JsonObject();
                vehicleData.addProperty("make", vehicle.getVehicleMake());
                vehicleData.addProperty("plate", vehicle.getPlateNumber());
                vehicleData.add("propertyRegistration", fileToJson(vehicle.getPropertyRegistryDocument()));
                vehicleData.add("authorizationDeclaration", fileToJson(vehicle.getDeclarationDocument()));

                vehiclesData.add(vehicleData);
            }
            parkingPartyData.add("vehicles", vehiclesData);

            // Notes
            parkingPartyData.addProperty("notes", party.getNotes());

            // Requests
            JsonArray requestsData = new JsonArray();
            for (ParkingRequest request : party.getOrderedParkingRequests()) {
                JsonObject requestData = new JsonObject();
                requestData.addProperty("creation", request.getCreationDate().toString());
                requestData.addProperty("email", request.getEmail());
                requestData.addProperty("phoneNr", request.getMobile());
                requestData.addProperty("limitless", request.getLimitlessAccessCard());
                requestData.addProperty("notes", request.getNote());

                requestsData.add(requestData);
            }
            parkingPartyData.add("requests", requestsData);

            // History
            JsonArray historyData = new JsonArray();
            for (ParkingPartyHistory historyEntry : party.getParty().getParkingPartyHistoriesSet()) {
                JsonObject historyEntryData = new JsonObject();
                historyEntryData.addProperty("creation", historyEntry.getHistoryDate().toString());

                JsonObject cardData = new JsonObject();
                String start = historyEntry.getCardStartDate() == null ? null : party.getCardStartDate().toString();
                String end = historyEntry.getCardEndDate() == null ? null : party.getCardEndDate().toString();

                cardData.addProperty("number", card);
                cardData.addProperty("start", start);
                cardData.addProperty("end", end);

                historyEntryData.add("card", cardData);

                historyEntryData.addProperty("notes", historyEntry.getNotes());

                historyData.add(historyEntryData);
            }
            parkingPartyData.add("history", historyData);

            parkingPartiesData.add(parkingPartyData);
        }
        allData.add("parkingParties", parkingPartiesData);

        BufferedWriter writer = new BufferedWriter(new FileWriter(exportTo));
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(allData).toString());
        writer.close();
    }

    private JsonObject fileToJson(NewParkingDocument file) {
        JsonObject fileData = new JsonObject();
        if (file == null) {
            fileData.addProperty("name", (String) null);
            fileData.addProperty("data", (String) null);

            return fileData;
        }
        String fileBase64 = Base64.getEncoder().encodeToString(file.getParkingFile().getContent());
        fileData.addProperty("name", file.getParkingFile().getFilename());
        fileData.addProperty("data", fileBase64);

        return fileData;
    }

}
