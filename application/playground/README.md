---
title: Playground
group: Applications
---
# Eclipse Daanse Playground Application

**Availability: Git Repository Only - No Docker Container/Artifact**

The playground application is a comprehensive development tool that includes all components necessary for creating and configuring Daanse OLAP servers. It serves as a complete testing and development environment.

## What's Included

### Complete Component Set
- **All Dialects**: Support for every database dialect available in the Daanse ecosystem
- **All Data Sources**: Every possible data source connector included
- **Web Consoles**: Administrative and management web interfaces
- **Helper Components**: Tools that assist in creating and configuring pivot servers

### Development Features
- **Full Configuration Testing**: Test all possible configurations before production deployment
- **Component Exploration**: Discover and experiment with different Daanse components
- **Integration Testing**: Validate component interactions in a complete environment

## Security Considerations

### Why No Docker Release
Due to the large number of components included in the playground application, the attack surface is significantly increased. For this reason, the Daanse project has decided not to provide a Docker release or pre-built artifacts.

### Risk Assessment
- **Increased Attack Surface**: More components mean more potential security vulnerabilities
- **Development Only**: Not intended for production environments
- **Security Trade-off**: Comprehensive functionality vs. security exposure

## Use Cases

### Development Tool
- **Server Customization**: Ideal for adapting and developing Daanse OLAP servers
- **Component Selection**: Help determine which components are needed for specific use cases
- **Configuration Validation**: Test configurations before implementing in production

### Learning Environment
- **Feature Exploration**: Understand all available Daanse capabilities
- **Component Dependencies**: Learn how different components interact
- **Best Practices**: Develop understanding of optimal configurations

## Getting Started

### Source Code Location
The playground application source can be found at:
`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/playground`

### Prerequisites
- Access to the Daanse git repository
- Java development environment
- Understanding of security implications

## Important Notes

⚠️ **Security Warning**: This application should only be used in development environments due to the increased attack surface from the comprehensive component set.

✅ **Development Value**: Despite security considerations, this is a very helpful tool for customizing and developing Daanse OLAP servers.

🔧 **Configuration Helper**: Essential for understanding component relationships and optimal server configurations before production deployment.