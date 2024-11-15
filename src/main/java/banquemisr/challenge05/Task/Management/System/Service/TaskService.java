package banquemisr.challenge05.Task.Management.System.Service;

import banquemisr.challenge05.Task.Management.System.Dto.Page.PageDto;
import banquemisr.challenge05.Task.Management.System.Dto.Search.TaskSearchDTOAdmin;
import banquemisr.challenge05.Task.Management.System.Dto.Search.TaskSearchDTOUser;
import banquemisr.challenge05.Task.Management.System.Dto.Search.TaskSearchResultDto;
import banquemisr.challenge05.Task.Management.System.Exception.TaskNotFoundException;
import banquemisr.challenge05.Task.Management.System.Exception.UserMisMatchException;
import banquemisr.challenge05.Task.Management.System.Mapper.TaskMapper;
import banquemisr.challenge05.Task.Management.System.Repository.TaskRepository;
import banquemisr.challenge05.Task.Management.System.Repository.TaskSpecification;
import banquemisr.challenge05.Task.Management.System.Dto.Task.AddTaskDTO;
import banquemisr.challenge05.Task.Management.System.Dto.Task.TaskDto;
import banquemisr.challenge05.Task.Management.System.Dto.Task.UpdateTaskDTO;
import banquemisr.challenge05.Task.Management.System.Entity.Task.Priority;
import banquemisr.challenge05.Task.Management.System.Entity.Task.Status;
import banquemisr.challenge05.Task.Management.System.Entity.Task.Task;
import banquemisr.challenge05.Task.Management.System.Entity.User.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final AuthenticationService authenticationService;
    private final UserService userService;


    protected User getLoggedInUser() {
        UserDetails loggedInUser = authenticationService.getCurrentUser().orElseThrow(() -> new IllegalArgumentException("User Not Found"));
        User user = userService.findByEmail(loggedInUser.getUsername());
        return user;
    }

    public TaskDto saveTask(AddTaskDTO taskRequestDTO) {
        Task task = taskMapper.toEntity(taskRequestDTO);
        task.setUser(getLoggedInUser());
        Task savedTask = taskRepository.save(task);
        return taskMapper.toDto(savedTask);
    }

    public List<TaskDto> getAllTaskForLoggedInUser() {
        User user = getLoggedInUser();
        return taskMapper.toDtos(findTasksByUser(user));
    }

    public TaskDto getTaskById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID must be greater than 0");
        }
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        if (!task.getUser().equals(getLoggedInUser())) { // Logged-In user doesn't own the task
            throw new UserMisMatchException("Invalid Logged in User");
        }
        // We can use something like existsByIdAndUser , but how to return correct exception

        return taskMapper.toDto(task);
    }

    public TaskDto updateTask(Long id, UpdateTaskDTO updateTaskDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID must be greater than 0");
        }
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        User user = getLoggedInUser();

        // Check if the Logged-In user is the owner of the task
        if (!task.getUser().equals(user)) {
            throw new UserMisMatchException("Invalid Logged in User");
        }

        Task updatedTask = taskMapper.toEntity(updateTaskDTO);
        updatedTask.setId(id);
        updatedTask.setUser(user);
        Task savedTask = taskRepository.save(updatedTask);
        return taskMapper.toDto(savedTask);

    }

    public void deleteTask(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID must be greater than 0");
        }
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        User user = getLoggedInUser();

        if (!task.getUser().equals(user)) {
            throw new UserMisMatchException("Invalid Logged in User");
        }
        taskRepository.deleteById(id);
    }

    public Task getTaskEntityById(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    public TaskDto changeStatus(Long id, String newStatus) {
        Status status = Status.valueOf(newStatus);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        User user = getLoggedInUser();
        if (!task.getUser().equals(user)) {
            throw new UserMisMatchException("Invalid Logged in User");
        }

        task.setStatus(status);
        task.setUser(user);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toDto(savedTask);
    }

    public TaskDto changePriority(Long id, String newPriority) {
        Priority priority = Priority.valueOf(newPriority);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        User user = getLoggedInUser();
        if (!task.getUser().equals(user)) {
            throw new UserMisMatchException("Invalid Logged in User");
        }
        task.setPriority(priority);
        task.setUser(user);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toDto(savedTask);
    }

    public List<TaskDto> getTasksDueDate(LocalDateTime date) {
        User user = getLoggedInUser();
        return taskMapper.toDtos(taskRepository.findAllByUserAndDueDateBefore(user, date));
    }

    private List<Task> findTasksByUser(User user) {
        return taskRepository.findAllByUser(user);
    }

    public PageDto<TaskDto> adminGetAllTasks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage = taskRepository.findAll(pageable);

        List<TaskDto> taskDtos = taskMapper.toDtos(taskPage.getContent());

        return new PageDto<>(
                taskDtos,
                taskPage.getTotalPages(),
                taskPage.getTotalElements(),
                taskPage.getSize(),
                taskPage.getNumber()
        );
    }

    public List<TaskDto> userSearchTasks(TaskSearchDTOUser taskSearchDTO) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasUser(getLoggedInUser()))
                .and(TaskSpecification.hasTitleContaining(taskSearchDTO.getTitle()))
                .and(TaskSpecification.hasDescriptionContaining(taskSearchDTO.getDescription()))
                .and(TaskSpecification.hasStatus(taskSearchDTO.getStatus()))
                .and(TaskSpecification.hasPriority(taskSearchDTO.getPriority()))
                .and(TaskSpecification.hasDueDateBetween(taskSearchDTO.getStartDate(), taskSearchDTO.getEndDate()));

        List<Task> taskList = taskRepository.findAll(spec);
        List<TaskDto> taskDtos = taskMapper.toDtos(taskList);
        return taskDtos;
    }

    public List<TaskSearchResultDto> adminSearchTasks(TaskSearchDTOAdmin taskSearchDTO) {
        Specification<Task> spec = Specification.where(null); // Start with a base Specification of `null`

        // If userId is provided, add user filtering
        if (taskSearchDTO.getUserId() != null) {
            User user = userService.findById(taskSearchDTO.getUserId());
            spec = spec.and(TaskSpecification.hasUser(user));
        }
        spec = spec.and(TaskSpecification.hasTitleContaining(taskSearchDTO.getTitle()))
                .and(TaskSpecification.hasDescriptionContaining(taskSearchDTO.getDescription()))
                .and(TaskSpecification.hasStatus(taskSearchDTO.getStatus()))
                .and(TaskSpecification.hasPriority(taskSearchDTO.getPriority()))
                .and(TaskSpecification.hasDueDateBetween(taskSearchDTO.getStartDate(), taskSearchDTO.getEndDate()));

        List<Task> taskList = taskRepository.findAll(spec);
        List<TaskSearchResultDto> resultDto = taskList.stream()
                .map(TaskSearchResultDto::from).toList();

        return resultDto;
    }


}
