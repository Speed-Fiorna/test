package com.example.difytest.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseData {

    private String task_id;
    private String workflow_run_id;
    private Data data;

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public  static class Data {
        private String id;
        private String workflow_id;
        private String status;
        private Outputs outputs;
        private Double elapsed_time;
        private Integer total_tokens;
        private Integer total_steps;
        private Long created_at;
        private Long finished_at;

        private String error;  // 添加 error 字段
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Outputs {
        private String finalOutput;

        @JsonProperty("final")
        public String getFinalOutput() {
            return finalOutput;
        }

        public void setFinalOutput(String finalOutput) {
            this.finalOutput = finalOutput;
        }
    }

}
