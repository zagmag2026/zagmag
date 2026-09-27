package com.nimsdeveloper.zhagmagdresses.admin

/**
 * Process-local targeted freshness coordinator.
 *
 * Mutations never poll. They only advance the domains that became stale. Each screen/view-model
 * consumes the relevant revision on its next normal entry/refresh. Dashboard is revision-based too; off-screen mutations never trigger a background API call.
 */
object AdminDataFreshness {
    var dashboardRevision: Int = 0
        private set
    var bookingsRevision: Int = 0
        private set
    var customersRevision: Int = 0
        private set
    var inventoryRevision: Int = 0
        private set
    var bookingBootstrapRevision: Int = 0
        private set
    var reportsMetadataRevision: Int = 0
        private set
    var reportsDataRevision: Int = 0
        private set
    var usersRevision: Int = 0
        private set
    var auditRevision: Int = 0
        private set

    fun markSessionBoundary() {
        dashboardRevision += 1
        bookingsRevision += 1
        customersRevision += 1
        inventoryRevision += 1
        bookingBootstrapRevision += 1
        reportsMetadataRevision += 1
        reportsDataRevision += 1
        usersRevision += 1
        auditRevision += 1
    }

    fun markBookingMutation() {
        dashboardRevision += 1
        bookingsRevision += 1
        customersRevision += 1
        inventoryRevision += 1
        reportsDataRevision += 1
        auditRevision += 1
    }

    fun markCustomerMutation() {
        dashboardRevision += 1
        customersRevision += 1
        bookingsRevision += 1
        bookingBootstrapRevision += 1
        reportsDataRevision += 1
        auditRevision += 1
    }

    fun markInventoryMutation() {
        dashboardRevision += 1
        inventoryRevision += 1
        bookingsRevision += 1
        bookingBootstrapRevision += 1
        reportsMetadataRevision += 1
        reportsDataRevision += 1
        auditRevision += 1
    }

    fun markBillingMutation() {
        dashboardRevision += 1
        reportsDataRevision += 1
        auditRevision += 1
    }

    fun markReportsMetadataMutation() {
        reportsMetadataRevision += 1
        auditRevision += 1
    }

    fun markUserMutation() {
        usersRevision += 1
        reportsMetadataRevision += 1
        reportsDataRevision += 1
        auditRevision += 1
    }

    fun markAuditMutation() {
        auditRevision += 1
    }

    /** Password-only changes do not change list/report metadata, but do add an Audit row. */
    fun markPasswordMutation() {
        auditRevision += 1
    }
}
