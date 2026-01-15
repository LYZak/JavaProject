// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol.profile;

public class UsageLimit {
    private Integer perUserPerDayMax;
    private Integer perUserPerWeekMax;
    private Integer perUserPerMonthMax;
    private Integer globalPerDayMax;
    private Integer globalPerWeekMax;
    private Integer globalPerMonthMax;
    private boolean perUserPerDayPerResource;

    public Integer getPerUserPerDayMax() {
        return perUserPerDayMax;
    }

    public void setPerUserPerDayMax(Integer perUserPerDayMax) {
        this.perUserPerDayMax = perUserPerDayMax;
    }

    public Integer getPerUserPerWeekMax() {
        return perUserPerWeekMax;
    }

    public void setPerUserPerWeekMax(Integer perUserPerWeekMax) {
        this.perUserPerWeekMax = perUserPerWeekMax;
    }

    public Integer getPerUserPerMonthMax() {
        return perUserPerMonthMax;
    }

    public void setPerUserPerMonthMax(Integer perUserPerMonthMax) {
        this.perUserPerMonthMax = perUserPerMonthMax;
    }

    public Integer getGlobalPerDayMax() {
        return globalPerDayMax;
    }

    public void setGlobalPerDayMax(Integer globalPerDayMax) {
        this.globalPerDayMax = globalPerDayMax;
    }

    public Integer getGlobalPerWeekMax() {
        return globalPerWeekMax;
    }

    public void setGlobalPerWeekMax(Integer globalPerWeekMax) {
        this.globalPerWeekMax = globalPerWeekMax;
    }

    public Integer getGlobalPerMonthMax() {
        return globalPerMonthMax;
    }

    public void setGlobalPerMonthMax(Integer globalPerMonthMax) {
        this.globalPerMonthMax = globalPerMonthMax;
    }

    public boolean isPerUserPerDayPerResource() {
        return perUserPerDayPerResource;
    }

    public void setPerUserPerDayPerResource(boolean perUserPerDayPerResource) {
        this.perUserPerDayPerResource = perUserPerDayPerResource;
    }
}

