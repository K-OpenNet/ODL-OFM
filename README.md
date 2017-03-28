# Overload and Failure Management (OFM) Module

An overload and failure management (OFM) module in SFC that consists of the overload management (OM) module and the failure management (FM) module.

In the OM module, when the current load at an SF instance exceeds a low-level threshold, a backup SF instance is prepared in advance. Meanwhile, if the current load further exceeds a high-level threshold, flow migration from the current SF instance to the backup SF instance is triggered.

The FM module detects the failure of the SF instance by using a failure alarm. Upon detecting the failure, flow migration to the backup SF instance is triggered.

