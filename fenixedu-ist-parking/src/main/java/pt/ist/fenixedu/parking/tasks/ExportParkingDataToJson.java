package pt.ist.fenixedu.parking.tasks;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.Interval;

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
        final JsonObject allData = new JsonObject();

        // Parking Groups
        final JsonArray parkingGroupsData = new JsonArray();
        for (final ParkingGroup group : ParkingGroup.getAll()) {
            final JsonObject parkingGroupData = new JsonObject();
            parkingGroupData.addProperty("name", group.getGroupName());

            // Parking Parties
            final JsonArray parkingPartiesData = new JsonArray();
            for (final ParkingParty party : group.getParkingPartiesSet()) {
                final JsonObject parkingPartyData = parkingPartyToJson(party);
                if (parkingPartyData != null) {
                    parkingPartiesData.add(parkingPartyData);
                }
            }
            parkingGroupData.add("parkingParties", parkingPartiesData);

            parkingGroupsData.add(parkingGroupData);
        }
        allData.add("parkingGroups", parkingGroupsData);

        final BufferedWriter writer = new BufferedWriter(new FileWriter(exportDir + exportFilename));
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(allData).toString());
        writer.close();
    }

    private JsonObject parkingPartyToJson(ParkingParty party) {
        if (!checkParty(party)) {
            return null;
        }

        final Person person = (Person) party.getParty();
        final String driverUsername = person.getUsername();
        final JsonObject parkingPartyData = new JsonObject();
        parkingPartyData.addProperty("username", driverUsername);
        parkingPartyData.addProperty("phoneNr", party.getParty().getDefaultMobilePhoneNumber());
        parkingPartyData.addProperty("email", party.getParty().getDefaultEmailAddressValue());

        // Driver's License
        parkingPartyData.add("driversLicense", fileToJson(party.getDriverLicenseDocument(), driverUsername));

        // Access Card
        final Long card = party.getCardNumber();
        if (card == null) {
            this.getLogger().info("Parking Party {} ({}) has no Access Card.", party.getExternalId(), driverUsername);
            taskLog("Parking Party %s (%s) has no Access Card.\n", party.getExternalId(), driverUsername);
        } else {
            final JsonObject cardData = new JsonObject();
            final String start = party.getCardStartDate() == null ? null : party.getCardStartDate().toString();
            final String end = party.getCardEndDate() == null ? null : party.getCardEndDate().toString();

            cardData.addProperty("number", card);
            cardData.addProperty("start", start);
            cardData.addProperty("end", end);

            parkingPartyData.add("card", cardData);
        }

        // Vehicles
        final JsonArray vehiclesData = new JsonArray();
        int vehicleNr = 1;
        for (final Vehicle vehicle : party.getVehiclesSet()) {
            final JsonObject vehicleData = new JsonObject();
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
        final JsonArray requestsData = new JsonArray();
        for (final ParkingRequest request : party.getOrderedParkingRequests()) {
            final JsonObject requestData = new JsonObject();
            requestData.addProperty("creation", request.getCreationDate().toString());
            requestData.addProperty("email", request.getEmail());
            requestData.addProperty("phoneNr", request.getMobile());
            requestData.addProperty("requestedAs", request.getRequestedAs());
            requestData.addProperty("notes", request.getNote());

            requestsData.add(requestData);
        }
        parkingPartyData.add("requests", requestsData);

        // History
        final JsonArray historyData = new JsonArray();
        for (final ParkingPartyHistory historyEntry : party.getParty().getParkingPartyHistoriesSet()) {
            final JsonObject historyEntryData = new JsonObject();
            historyEntryData.addProperty("creation", historyEntry.getHistoryDate().toString());

            final JsonObject cardData = new JsonObject();
            final String start = historyEntry.getCardStartDate() == null ? null : party.getCardStartDate().toString();
            final String end = historyEntry.getCardEndDate() == null ? null : party.getCardEndDate().toString();

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

    public boolean checkParty(ParkingParty party) {
        if (party.getParty() == null) {
            this.getLogger().warn("Parking Party {} without associated Party. Skipping.", party.getExternalId());
            taskLog("Parking Party %s without associated Party. Skipping.\n", party.getExternalId());
            return false;
        }
        if (!party.getParty().isPerson()) {
            this.getLogger().warn("Parking Party {} with associated non-Person Party. Skipping.", party.getExternalId());
            taskLog("Parking Party %s with associated non-Person Party. Skipping.\n", party.getExternalId());
            return false;
        }
        final Person person = (Person) party.getParty();
        if (StringUtils.isBlank(person.getDefaultEmailAddressValue())) {
            if (party.getCardNumber() == null || party.getCardStartDate() == null || party.getCardEndDate() == null) {
                this.getLogger().warn("{} ({}) has no email address and no card. Skipping.", person.getUsername(),
                        person.getName());
                taskLog("%s (%s) has no email address and no card. Skipping.\n", person.getUsername(), person.getName());
                return false;
            }
            final Interval cardInterval = new Interval(party.getCardStartDate(), party.getCardEndDate());
            this.getLogger().warn("{} ({}) has no email address and has an "
                    + (cardInterval.containsNow() ? "ACTIVE" : "inactive") + " card. Skipping.", person.getUsername(),
                    person.getName());
            taskLog("%s (%s) has no email address and and has an " + (cardInterval.containsNow() ? "ACTIVE" : "inactive")
                    + " card. Skipping.\n", person.getUsername(), person.getName());
            return false;
        }
        return true;
    }

    private JsonObject fileToJson(NewParkingDocument file, String prefix) {
        if (file == null) {
            return null;
        }

        final String filename = file.getParkingFile().getFilename();
        final String id = file.getParkingFile().getExternalId();

        final JsonObject fileData = new JsonObject();
        fileData.addProperty("name", filename);
        fileData.addProperty("id", id);

        return fileData;
    }

}
