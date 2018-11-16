package pt.ist.fenixedu.parking.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
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

    private final String exportDir = "/path/to/file/";
    private final String exportFilename = "parking.json";

    @Override
    public void runTask() throws Exception {
        JsonObject allData = new JsonObject();

        // Parking Groups
        JsonArray parkingGroupsData = new JsonArray();
        for (ParkingGroup group : ParkingGroup.getAll()) {
            JsonObject parkingGroupData = new JsonObject();
            parkingGroupData.addProperty("name", group.getGroupName());

            // Parking Parties
            JsonArray parkingPartiesData = new JsonArray();
            for (ParkingParty party : group.getParkingPartiesSet()) {
                JsonObject parkingPartyData = parkingPartyToJson(party);
                if (parkingPartyData != null) {
                    parkingPartiesData.add(parkingPartyData);
                }
            }
            parkingGroupData.add("parkingParties", parkingPartiesData);

            parkingGroupsData.add(parkingGroupData);
        }
        allData.add("parkingGroups", parkingGroupsData);
        

        BufferedWriter writer = new BufferedWriter(new FileWriter(exportDir + exportFilename));
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(allData).toString());
        writer.close();
    }

    private JsonObject parkingPartyToJson(ParkingParty party) {
        if (party.getParty() == null) {
            this.getLogger().warn("Parking Party {} without associated Party. Skipping.", party.getExternalId());
            taskLog("Parking Party %s without associated Party. Skipping.\n", party.getExternalId());
            return null;
        }
        if (!party.getParty().isPerson()) {
            this.getLogger().warn("Parking Party {} with associated non-Person Party. Skipping.", party.getExternalId());
            taskLog("Parking Party %s with associated non-Person Party. Skipping.\n", party.getExternalId());
            return null;
        }
        Person person = (Person) party.getParty();
        if (person.getSocialSecurityNumber() == null) {
            this.getLogger().warn("{} ({}) has no Social Security Number. Skipping.", person.getUsername(), person.getName());
            taskLog("%s (%s) has no Social Security Number. Skipping.\n", person.getUsername(), person.getName());
            return null;
        }
        String driverUsername = person.getUsername();
        JsonObject parkingPartyData = new JsonObject();
        parkingPartyData.addProperty("username", driverUsername);
        parkingPartyData.addProperty("phoneNr", party.getParty().getDefaultMobilePhoneNumber());
        parkingPartyData.addProperty("email", party.getParty().getDefaultEmailAddressValue());

        // Driver's License
        parkingPartyData.add("driversLicense", fileToJson(party.getDriverLicenseDocument(), driverUsername));

        // Access Card
        Long card = party.getCardNumber();
        if (card == null) {
            this.getLogger().info("Parking Party {} ({}) has no Access Card.", party.getExternalId(), driverUsername);
            taskLog("Parking Party %s (%s) has no Access Card.\n", party.getExternalId(), driverUsername);
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
        int vehicleNr = 1;
        for (Vehicle vehicle : party.getVehiclesSet()) {
            JsonObject vehicleData = new JsonObject();
            vehicleData.addProperty("make", vehicle.getVehicleMake());
            vehicleData.addProperty("plate", vehicle.getPlateNumber());
            vehicleData.add("propertyRegistration",
                    fileToJson(vehicle.getPropertyRegistryDocument(), driverUsername + "_v" + vehicleNr + "_p"));
            vehicleData.add("authorizationDeclaration",
                    fileToJson(vehicle.getDeclarationDocument(), driverUsername + "_v" + vehicleNr + "_a"));

            vehiclesData.add(vehicleData);
            vehicleNr++;
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
            requestData.addProperty("requestedAs", request.getRequestedAs());
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

            cardData.addProperty("number", historyEntry.getCardNumber());
            cardData.addProperty("start", start);
            cardData.addProperty("end", end);

            historyEntryData.add("card", cardData);

            historyEntryData.addProperty("requestedAs", historyEntry.getRequestedAs());
            historyEntryData.addProperty("usedNumber", historyEntry.getUsedNumber());
            historyEntryData.addProperty("notes", historyEntry.getNotes());

            historyData.add(historyEntryData);
        }
        parkingPartyData.add("history", historyData);

        return parkingPartyData;
    }

    private JsonObject fileToJson(NewParkingDocument file, String prefix) {
        if (file == null) {
            return null;
        }
        
        // copy file to export folder
        String filename = file.getParkingFile().getFilename();
        String path = exportDir + prefix + "_" + filename;
        try {
            FileUtils.writeByteArrayToFile(new File(path), file.getParkingFile().getContent());
        } catch (IOException e) {
            this.getLogger().error("Unable to export file %s", filename);
            taskLog("Unable to export file %s", filename);
            return null;
        }

        JsonObject fileData = new JsonObject();
        fileData.addProperty("name", filename);
        fileData.addProperty("path", path);

        return fileData;
    }

}
