package com.saurav.schedulingservice.controller;

import com.saurav.schedulingservice.service.LeaderElectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeaderController {

    private final LeaderElectionService leaderElectionService;

     public LeaderController(LeaderElectionService leaderElectionService){
        this.leaderElectionService=leaderElectionService;
    }

    @GetMapping("/is-leader")
    public String isLeader() {
        return leaderElectionService.isLeader() ? "I am the leader" : "I am a follower";
    }
}
