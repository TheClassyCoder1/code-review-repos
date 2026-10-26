package com.example.lending.ops.part4.members;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** Banner shown on the borrower app home page (rate promotions, refinance offers). */
@Data
public class SmsHomeAdvertise implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    /** 0 = web portal, 1 = mobile app. */
    private Integer type;
    private String pic;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private Integer clickCount;
    private Integer orderCount;
    private String url;
    private String note;
    private Integer sort;
}
