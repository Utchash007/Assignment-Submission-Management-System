package com.asms.springasms.dto.course;

import com.asms.springasms.entity.Course;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String code,
        String title,
        String description
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getCode(), course.getTitle(), course.getDescription());
    }
}
