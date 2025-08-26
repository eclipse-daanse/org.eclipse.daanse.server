---
title: Pivot
group: Applications
---
# Eclipse Daanse Pivor Application

**Status: Under Construction**

Pivot is the foundation for every production Daanse OLAP server. This container can be adapted to specific needs and requirements.

## Key Configuration Areas

### Database Connectivity
- **Dialects and Data Sources**: Support for multiple different database connections
- **Multi-Database Support**: Can connect to various database types simultaneously

### XMLA Endpoints
- **Multiple Endpoints**: Configure the number of XMLA endpoints
- **Path Configuration**: Customize endpoint paths
- **Port Mapping**: Assign specific ports to different endpoints

### Catalog Management
- **Web Endpoint Assignment**: Map catalogs to specific web endpoints
- **Flexible Routing**: Configure how catalogs are accessed through different entry points

### Observability Features
- **Health Checks**: Built-in health monitoring capabilities  
- **Readiness Checks**: Ensure services are ready to handle requests
- **Caching Configuration**: Configurable caching settings for optimal performance

### Security & Authentication
- **Authentication Providers**: Support for necessary authentication mechanisms
- **Full Control**: Complete control over security configurations
- **Customizable Access**: Adapt authentication to organizational needs

## Benefits

### Security & Risk Reduction
This container provides full control and adaptability while ensuring that unnecessary components are not activated at runtime, significantly reducing security risks.

### Production Ready
Designed specifically for production environments with enterprise-grade features and reliability.

### Customization
Every aspect can be tailored to meet specific organizational requirements and use cases.

## Source Code
The pivot application source can be found at: `https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot`