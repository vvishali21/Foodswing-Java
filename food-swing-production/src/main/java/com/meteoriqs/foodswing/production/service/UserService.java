package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.production.record.UserRecord;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class UserService {

    public UserRecord createUser(UserRecord record) {
        log.debug("Create user request: {}", record);
        return new UserRecord(record.username(), record.firstName(), record.lastName(), true);
    }
}
