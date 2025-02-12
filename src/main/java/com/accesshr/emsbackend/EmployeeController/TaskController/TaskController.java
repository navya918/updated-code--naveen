package com.accesshr.emsbackend.EmployeeController.TaskController;



import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.accesshr.emsbackend.Entity.LeaveRequest;
import com.accesshr.emsbackend.Entity.Task;
import com.accesshr.emsbackend.Service.LeaveService.LeaveRequestServiceImpl;
import com.accesshr.emsbackend.Service.TaskService.*;
import com.accesshr.emsbackend.Util.HolidaysUtil;
import com.accesshr.emsbackend.exceptions.ResourceNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;



  @RestController

  @CrossOrigin
  @RequestMapping("apis/employees")
public class TaskController {
    @Autowired
    public TaskJpaService taskService;

    @GetMapping(value="/tasks",produces = "application/json")
    public ArrayList<Task> gettTasks(){
        return taskService.getTasks();
    }

    @GetMapping(value="/tasks/{taskId}",produces = "application/json")

    public Task getTaskById(@PathVariable int taskId){
      return taskService.getTaskById(taskId);
    }

    @GetMapping(value="/tasksAssignedBy/{email}",produces = "application/json")

    public List<Task> getTaskAssignedBy(@PathVariable String email){
      return taskService.getTasksAssignedForm(email);
    }

    @GetMapping(value="/tasksAssignedTo/{email}",produces = "application/json")

    public List<Task> getTaskAssignedTo(@PathVariable String email){
      return taskService.getTasksAssignedTo(email);
    }

    @GetMapping(value="/tasksCount/{email}",produces = "application/json")

    public int countOfTasks(@PathVariable String email){
      return taskService.countOfTasks(email);
    }

    @PostMapping(value="/tasks",produces = "application/json")

    public Task addTask(@RequestBody Task task){
      return taskService.addTask(task);
    }

    @PutMapping(value="/tasks/{taskId}",produces = "application/json")

    public Task updateTask(@PathVariable int taskId,@RequestBody Task task){
      return taskService.updateTask(taskId,task);
    }

    @DeleteMapping(value="/tasks/{taskId}",produces = "application/json")

    public void deleteTask(@PathVariable int taskId){
      taskService.deleteTask(taskId);
    }

    @GetMapping(value="/OverdueTasks/AssignedFrom/{email}",produces = "application/json")
    public List getOverDueTasksAssignedFrom(@PathVariable String email) {
        return taskService.getOverDueTasksAssignedFrom(email);
    }

    @GetMapping(value="/PendingTasks/AssignedFrom/{email}",produces = "application/json")
    public List getPendingTasksAssignedFrom(@PathVariable String email) {
        return taskService.getPendingTasksAssignedFrom(email);
    }

    @GetMapping(value="/CompletedTasks/AssignedFrom/{email}",produces = "application/json")
    public List getCompletedTasksAssignedFrom(@PathVariable String email) {
        return taskService.getCompletedTasksAssignedFrom(email);
    }

    @GetMapping(value="/OverdueTasks/PersonId/{personId}",produces = "application/json")
    public List getOverDuePersonEmail(@PathVariable String personId) {
        return taskService.getOverDueTasksPersonId(personId);
    }

    @GetMapping(value="/PendingTasks/PersonId/{personId}",produces = "application/json")
    public List getPendingPersonEmail(@PathVariable String personId) {
        return taskService.getPendingTasksPersonId(personId);
    }

    @GetMapping(value="/CompletedTasks/PersonId/{personId}",produces = "application/json")
    public List getCompletedTasksPersonId(@PathVariable String personId) {
        return taskService.getCompletedTasksPersonId(personId);
    }

    @GetMapping(value="/TasksDetails/PersonId/{personId}",produces = "application/json")
    public HashMap<String,Integer> getTasksEfficiency(@PathVariable String personId) {
        return taskService.getTasksEfficiency(personId);
    }

    

    @Autowired
    private LeaveRequestServiceImpl leaveRequestServiceImpl;

      @Value("${azure.storage.connection-string}")
      private String connectionString;

      @Value("${azure.storage.container-name}")
      private String containerName;

    @PostMapping(value = "/leave/submit", produces = "application/json")
    public ResponseEntity<?> submitLeaveRequest(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("managerId") String managerId,
            @RequestParam("managerEmail") String managerEmail,
            @RequestParam(value = "comments", required = false) String comments,
            @RequestParam(value="durationType", required = false) String durationType,
            @RequestParam(value = "duration", required = false) Double duration,
            @RequestParam(value = "LOP", required = false) boolean LOP,
            @RequestParam("leaveStartDate") LocalDate leaveStartDate,
            @RequestParam("leaveEndDate") LocalDate leaveEndDate,
            @RequestParam(value = "leaveReason", required = false) String leaveReason,
            @RequestParam(value = "leaveStatus", required = false) LeaveRequest.LeaveStatus leaveStatus,
            @RequestParam(value = "leaveType", required = false) LeaveRequest.LeaveType leaveType,
            @RequestParam(value = "medicalDocument", required = false) MultipartFile medicalDocument) throws IOException {
        try {
            LeaveRequest leaveRequest=new LeaveRequest();
            leaveRequest.setEmployeeId(employeeId);
            leaveRequest.setFirstName(firstName);
            leaveRequest.setLastName(lastName);
            leaveRequest.setEmail(email);
            leaveRequest.setManagerId(managerId);
            leaveRequest.setManagerEmail(managerEmail);
            leaveRequest.setComments(comments);
            leaveRequest.setLeaveType(leaveType);
            leaveRequest.setLeaveStartDate(leaveStartDate);
            leaveRequest.setLeaveEndDate(leaveEndDate);
            leaveRequest.setLOP(LOP);
            leaveRequest.setLeaveReason(leaveReason);
            leaveRequest.setLeaveStatus(leaveStatus);
            leaveRequest.setLeaveType(leaveType);
//            leaveRequest.setDuration(duration);

            if (leaveType == LeaveRequest.LeaveType.SICK) {
                double requestedDays = leaveRequest.calculateBusinessDays(
                        leaveStartDate, leaveEndDate, HolidaysUtil.getNationalHolidays(leaveStartDate.getYear())
                );
                if (requestedDays > 2 && medicalDocument != null) {
                    String savedFilePath = uploadFIle(medicalDocument, "medicalDocument");
                    leaveRequest.setMedicalDocument(savedFilePath);
                }
            }
            LeaveRequest savedRequest = leaveRequestServiceImpl.submitLeaveRequest(leaveRequest);
            return new ResponseEntity<>(savedRequest, HttpStatus.OK);
        }catch (ResourceNotFoundException e){
            return new ResponseEntity<>(e.getMap(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

      private String saveFile(MultipartFile file, String fileType) throws IOException {
          if (file == null || file.isEmpty()) {
              throw new IllegalArgumentException("The file cannot be null or empty.");
          }

          // Create a BlobContainerClient
          BlobContainerClient blobContainerClient = new BlobClientBuilder()
                  .connectionString(connectionString)
                  .containerName(containerName)
                  .buildClient().getContainerClient();

          String originalFilename = file.getOriginalFilename();
          if (originalFilename == null) {
              throw new IllegalArgumentException("Original filename cannot be null.");
          }

          String blobName = fileType + "-" + originalFilename;

          // Log the blob name before uploading
          System.out.println("Uploading to blob name: " + blobName);

          // Upload the file to Blob Storage
          BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
          blobClient.upload(file.getInputStream(), file.getSize(), true);

          return blobName; // Return the blob name or URL if needed
      }
      private String saveOptionalFile(MultipartFile file, String fileType) throws IOException {
          if (file != null && !file.isEmpty()) {
              return saveFile(file, fileType);
          }
          return null;
      }

      public String uploadFIle(MultipartFile file, String caption) throws IOException{
          String blobFilename=file.getOriginalFilename();

          BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                  .connectionString(connectionString)
                  .buildClient();
          BlobClient blobClient=blobServiceClient
                  .getBlobContainerClient(containerName)
                  .getBlobClient(blobFilename);

          blobClient.upload(file.getInputStream(), file.getSize(), true);
          String fileUrl=blobClient.getBlobUrl();

          return fileUrl;
      }

    // Approving a leave request
    @PutMapping("/approve/{id}")
    public ResponseEntity<String> approveLeaveRequest(@PathVariable Long id) {
        leaveRequestServiceImpl.approveLeaveRequest(id);
        return ResponseEntity.ok("Leave Request Approved");
    }

    // Rejecting a leave request
    @PutMapping("/reject/{id}/{leaveReason}")
    public ResponseEntity<String> rejectLeaveRequest(@PathVariable Long id, @PathVariable String leaveReason) {
        leaveRequestServiceImpl.rejectLeaveRequest(id, leaveReason);
        return ResponseEntity.ok("Leave Request Rejected with Reason: " + leaveReason);
    }

    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<LeaveRequest> updateLeaveRequest(@PathVariable Long id, @RequestBody LeaveRequest leaveRequest) {
        LeaveRequest updatedLeaveRequest = leaveRequestServiceImpl.updateLeaveRequest(id, leaveRequest);
        return ResponseEntity.ok(updatedLeaveRequest);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteLeaveRequest(@PathVariable Long id) {
        String deleteRequest = leaveRequestServiceImpl.deleteLeaveRequest(id);
        return ResponseEntity.ok(deleteRequest);
    }

    @GetMapping("/remaining-leaves")
    public ResponseEntity<?> getRemainingLeaveDays(@RequestParam("employeeId") String employeeId,@RequestParam("leaveType") LeaveRequest.LeaveType leaveType) {
        double remainingLeaveDays = leaveRequestServiceImpl.getRemainingLeaveDays(employeeId, leaveType);
        return new ResponseEntity<>(remainingLeaveDays, HttpStatus.OK);
    }

    @GetMapping(value = "/getAllLeaves", produces = "application/json")
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {
        List<LeaveRequest> leaveRequest = leaveRequestServiceImpl.getAllLeaveRequests();
        if (leaveRequest.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(leaveRequest);
        }
        return new ResponseEntity<>(leaveRequest, HttpStatus.OK);
    }

    @GetMapping(value = "/{status}/manager/{managerId}", produces = "application/json")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequestsByStatus(@PathVariable String status, @PathVariable String managerId) {
        LeaveRequest.LeaveStatus leaveStatus;
        try {
            leaveStatus = LeaveRequest.LeaveStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        List<LeaveRequest> leaveRequests = leaveRequestServiceImpl.getLeaveRequestsByStatus(managerId, leaveStatus);
        if (leaveRequests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(leaveRequests);
        }
        return ResponseEntity.ok(leaveRequests);
    }

    @GetMapping(value = "/{status}/employee/{employeeId}", produces = "application/json")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequestByEmployeeStatus(@PathVariable String status, @PathVariable String employeeId) {
        LeaveRequest.LeaveStatus leaveStatus;
        try {
            leaveStatus = LeaveRequest.LeaveStatus.valueOf(status.toUpperCase());
        }catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        List<LeaveRequest> leaveRequests = leaveRequestServiceImpl.getLeaveRequestByEmployeeStatus(employeeId, leaveStatus);
        if (leaveRequests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(leaveRequests);
        }
        return ResponseEntity.ok(leaveRequests);
    }

    @GetMapping(value = "/manager/{managerId}", produces = "application/json")
    public ResponseEntity<List<LeaveRequest>> getAllMangerIds(@PathVariable String managerId) {
        List<LeaveRequest> leaveRequest = leaveRequestServiceImpl.getAllManagerId(managerId);
        if (leaveRequest.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(leaveRequest);
        }
        return new ResponseEntity<>(leaveRequest, HttpStatus.OK);
    }

    @GetMapping(value = "/employee/{employeeId}", produces = "application/json")
    public ResponseEntity<List<LeaveRequest>> getAllEmployeeIds(@PathVariable String employeeId) {
        List<LeaveRequest> leaveRequest = leaveRequestServiceImpl.getAllEmployeeId(employeeId);
        return ResponseEntity.ok(leaveRequest);
    }


    @GetMapping("/fileSize")
    public ResponseEntity<Map<String, Long>> getFileSize(@RequestParam String fileName) {
        try {
            BlobContainerClient blobContainerClient = new BlobClientBuilder()
                    .connectionString(connectionString)
                    .containerName(containerName)
                    .buildClient().getContainerClient();

            // Get the blob properties
            BlobProperties properties = blobContainerClient.getBlobClient(fileName).getProperties();

            // Prepare response with file size
            Map<String, Long> response = new HashMap<>();
            response.put("size", properties.getBlobSize()); // Size in bytes
            return ResponseEntity.ok(response);
        } catch (BlobStorageException e) {
            // Handle not found error
            if (e.getStatusCode() == 404) {
                Map<String, Long> response = new HashMap<>();
                response.put("size", 0L); // File not found, size is 0
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            // Handle other errors
            Map<String, Long> response = new HashMap<>();
            response.put("size", 0L); // Error occurred, size is 0
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            // Handle any other exceptions
            Map<String, Long> response = new HashMap<>();
            response.put("size", 0L); // Error occurred, size is 0
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    

}

