---
title: Probe
group: Applications
---
# Eclipse Daanse Probe Application

## What is Eclipse Daanse Probe?

Eclipse Daanse Probe is a **rapid development and testing platform** for OLAP (Online Analytical Processing) catalogs and multidimensional data analysis. It's designed as the perfect tool for:

- **🚀 Fast Catalog Development**: Quickly create, test, and iterate OLAP catalog designs
- **🎭 Mock Data Creation**: Build realistic test scenarios with CSV-based data simulation
- **📚 OLAP Learning**: Comprehensive tutorial collection for mastering multidimensional analysis concepts
- **🔧 Prototyping**: Rapid prototyping of business intelligence solutions

## Full OLAP Analysis Server

Probe is not just a development tool—it's a **complete OLAP analysis server** that implements the full Eclipse Daanse server stack:

### XMLA Protocol Support
- **✅ Full XMLA Compatibility**: Implements XML for Analysis (XMLA) web services
- **📊 Excel Integration**: Direct connectivity with Microsoft Excel pivot tables
- **🔗 BI Tool Support**: Compatible with any XMLA-capable business intelligence tool

### Excel Connection
Connect Microsoft Excel directly to your Probe server:

1. **Open Excel** → Data → Get Data → From Other Sources → From Analysis Services
2. **Use Excel's Analysis Services connector** with endpoint: `http://localhost:8080/xmla`
3. **Or use OLEDB - Connection String**: `Provider=MSOLAP;Data Source=http://localhost:8080/xmla`

### Enterprise-Grade Features
- **Multidimensional Queries**: Full MDX (Multidimensional Expressions) query support  
- **Cube Operations**: Drill-down, slice, dice, and pivot operations
- **Calculated Members**: Dynamic measure and dimension calculations
- **Security**: Role-based access control and data security
- **Caching**: Intelligent query result caching for performance

## Development-Focused Design

### Live Catalog Reloading
- **🔄 Hot Reload**: Modify catalogs and data files while the server is running
- **📁 File Watching**: Automatic detection of catalog changes
- **⚡ Instant Feedback**: See changes immediately without restarts

### Tutorial-Rich Environment
Daanse Server Tutorials ships with **80+ tutorial scenarios** covering:
- Basic cube and dimension modeling
- Advanced aggregation strategies  
- Parent-child hierarchies and time dimensions
- Security and access control patterns
- KPI and calculated member examples
- Virtual cubes and writeback functionality

Download the Full [Tutorial Pack](https://github.com/eclipse-daanse/eclipse-daanse.github.io/raw/refs/heads/main/docs/public/cubeserver/tutorial/zip/all-tutorials.zip) or Learn in our Tutorials [Online](https://daanse.org/cubeserver/tutorial/)

## Features

- **OLAP Analytics Server**: Full-featured multidimensional analysis capabilities
- **Tutorial Datasets**: Extensive collection of educational examples including:
  - Access control tutorials (catalog, column, cube, dimension, hierarchy, member, table)
  - Aggregation examples (aggregate tables, exclusions)
  - Calculated members and KPI definitions
  - Drill-through actions and data exploration
  - Parent-child hierarchies and time dimensions
  - Virtual cubes and writeback functionality
- **Security**: Runs as non-root user (UID/GID 1000)
- **Logging**: Configurable logging with logback
- **Data Catalogs**: Support for XMI catalog mappings and CSV data files

## Running the Probe Server

Eclipse Daanse Probe can be run in multiple ways depending on your needs and environment.


### Container-Based Execution

Use containers for production deployments, isolated environments, and easy distribution.

#### Quick Start with Containers

```bash
# Docker
docker run --name probe \
  --userns=keep-id \
  -v ~/temp/probe/catalog:/app/catalog/:rw,Z \
  -v ~/temp/probe/output:/app/output/:rw,Z \
  -p 8095:8080 \
  -it eclipsedaanse/probe:snapshot
```

##### Container Parameters Explained

- `--name probe`: Container name
- `-v ~/temp/probe/catalog:/app/catalog/:rw,Z`: Mount local catalog directory ( read-write with SELinux context)
- `-v ~/temp/probe/output:/app/output/:rw,Z`: Mount the output folder, e.g. for the generated documentation
- `-p 8095:8080`: Map host port 8095 to container port 8080 - the single HTTP server carrying the XMLA endpoint (`/xmla`)
- `-it`: Attach a terminal (use `-d` to run detached instead)
- `eclipsedaanse/probe:snapshot`: Container image


#### Docker Compose

Use Docker Compose for multi-service setups with databases, reverse proxies, and monitoring:

```yaml
services:
  probe:
    image: eclipsedaanse/probe:snapshot
    container_name: daanse-probe
    ports:
      - "8095:8080"
    volumes:
      - type: bind
        source: ~/temp/probe/catalog
        target: /app/catalog/
        bind:
          selinux: Z
      - type: bind
        source: ~/temp/probe/output
        target: /app/output/
        bind:
          selinux: Z
      - type: bind
        source: ./logs
        target: /app/log/
        bind:
          selinux: Z
    networks:
      - daanse-network
    user: "1000:1000"

networks:
  daanse-network:
    driver: bridge
    name: daanse-network
```

**Start services**
```text
docker-compose -f compose/docker-compose.yml up -d
```

## Internal Database Engine

The probe carries its own in-memory **DuckDB** database and loads the CSV
files of every catalog into it - no external database is needed or possible.

## System Properties

When running the JAR directly (outside the container), two system properties
adjust the probe. The XMLA endpoint is served on the single HTTP server on
port 8080 at `/xmla`.

| Property | Default | Description |
|---|---|---|
| `daanse.probe.catalog.dir` | `./catalog` | Directory the catalogs are read from (watched for changes) |
| `daanse.probe.requireLogin` | `false` | Challenge requests instead of accepting them anonymously |

```bash
java -Ddaanse.probe.requireLogin=true -Dlogback.configurationFile=./logback.xml -jar daanse.probe.jar
```

## Directory Structure

```text
/app/
├── daanse.probe.jar          # Main application JAR
├── start                     # Startup script
├── logback.xml              # Logging configuration
├── catalog/                 # Data catalogs and mappings
│   ├── */data/             # CSV data files
│   └── */mapping/          # XMI catalog definitions
├── import/                  # Drop zone for catalog imports
├── output/                  # Output folder, e.g. generated documentation and ODC files
└── log/                     # Application logs
```

## Catalog Folder Structure

The `/app/catalog/` directory is the core of the Eclipse Daanse Probe system. It contains OLAP catalogs that define multidimensional data models and their associated data files.

### Catalog Organization

Each catalog is organized in its own subdirectory with the following structure:

```text
catalog/
├── my-catalog/                    # Catalog name (directory)
│   ├── mapping/
│   │   └── catalog.xmi           # XMI mapping definition
│   └── data/                     # Database simulation
│       ├── table1.csv            # Default schema tables
│       ├── table2.csv
│       └── schema1/              # Named schema
│           ├── table3.csv        # Tables in schema1
│           └── table4.csv
```

### XMI Mapping Files

The `mapping/catalog.xmi` file describes the OLAP model structure:
- **Cubes**: Multidimensional data structures
- **Dimensions**: Ways to slice and analyze data
- **Measures**: Numeric values to be aggregated
- **Database Schema**: Logical table and column definitions
- **Table Mappings**: Relationships between logical and physical data

### Data Files (CSV Format)

The `data/` folder simulates database tables using a special CSV format:

#### CSV File Structure
```csv
"Column1","Column2","Column3"      # Line 1: Header (column names)
VARCHAR,INTEGER,DECIMAL           # Line 2: Data types
"Value1",42,123.45               # Line 3+: Data rows
"Value2",84,246.90
```

**Important:** Line 2 contains the database column types, not data values.

#### Supported Data Types
- `VARCHAR` - String values
- `INTEGER` - Whole numbers
- `DECIMAL` - Decimal numbers
- `BIGINT` - Large integers
- `BOOLEAN` - True/false values
- `DATE` - Date values
- `TIMESTAMP` - Date and time values

#### Schema Organization
- **Default Schema**: CSV files directly in `data/` folder
- **Named Schemas**: CSV files in `data/schema-name/` subfolders
- **Schema Names**: Subfolder names become database schema names

### Example: Minimal Cube Catalog

Here's a complete example of the `tutorial.cube.minimal` catalog:

#### Directory Structure
```text
tutorial.cube.minimal/
├── mapping/
│   └── catalog.xmi
└── data/
    └── Fact.csv
```

#### Mapping Definition (`catalog.xmi`)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:businessinformation="http://www.omg.org/spec/CWM/1.1/foundation/businessinformation" xmlns:relational="http://www.omg.org/spec/CWM/1.1/resource/relational" xmlns:rolapcat="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping/catalog" xmlns:rolapcube="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping/olap/cube" xmlns:rolapmeas="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping/olap/cube/measure" xmlns:rolapsrc="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping/database/source">
  <rolapcat:Catalog xmi:id="_catalog_cube_minimal" name="Daanse Tutorial - Cube Minimal" importedElement="_schema">
    <ownedElement xsi:type="rolapsrc:TableSource" xmi:id="_tablesource_fact" name="Fact" table="_table_fact"/>
    <ownedElement xsi:type="rolapcube:PhysicalCube" xmi:id="_physicalcube_minimalcube" name="MinimalCube" source="_tablesource_fact">
      <measureGroups xmi:id="_measuregroup_minimalcube" name="MinimalCube">
        <measures xsi:type="rolapmeas:SumMeasure" xmi:id="_summeasure_measure_sum" name="Measure-Sum" column="_column_fact_value"/>
      </measureGroups>
    </ownedElement>
    <ownedElement xsi:type="businessinformation:Description" xmi:id="_description_cube_minimal_documentation_und" name="Daanse Tutorial - Cube Minimal_documentation_und" body="Basic cube structure with measures only" language="und" type="documentation" modelElement="_catalog_cube_minimal"/>
  </rolapcat:Catalog>
  <relational:SQLSimpleType xmi:id="_sqlsimpletype_character_varying" name="CHARACTER VARYING" typeNumber="12"/>
  <relational:SQLSimpleType xmi:id="_sqlsimpletype_integer" name="INTEGER" typeNumber="4"/>
  <relational:Schema xmi:id="_schema" importer="_catalog_cube_minimal">
    <ownedElement xsi:type="relational:Table" xmi:id="_table_fact" name="Fact">
      <feature xsi:type="relational:Column" xmi:id="_column_fact_key" name="KEY" type="_sqlsimpletype_character_varying"/>
      <feature xsi:type="relational:Column" xmi:id="_column_fact_value" name="VALUE" type="_sqlsimpletype_integer"/>
    </ownedElement>
  </relational:Schema>
</xmi:XMI>
```

#### Data File (`data/Fact.csv`)
```csv
"KEY","VALUE"
VARCHAR,INTEGER
A,42
B,21
```

This creates:
- A cube named "MinimalCube"
- A fact table "Fact" with KEY and VALUE columns
- A sum measure aggregating the VALUE column
- Two data rows with keys A/B and values 42/21

### Schema Example

For catalogs with multiple schemas like `tutorial.database.schema`:

```text
tutorial.database.schema/
├── mapping/
│   └── catalog.xmi
└── data/
    ├── theTable.csv              # Default schema
    ├── theschema/
    │   └── theTable.csv          # "theschema" schema
    └── nonref/
        └── theTable.csv          # "nonref" schema
```

Each subfolder (`theschema`, `nonref`) becomes a database schema containing the CSV files within it.

### Creating Custom Catalogs

To create your own catalog:

1. **Create the directory structure**:
   ```bash
   mkdir -p my-catalog/{mapping,data}
   ```

2. **Define the XMI mapping** in `my-catalog/mapping/catalog.xmi`

3. **Add CSV data files** in `my-catalog/data/`


### Runtime Catalog Management

🔄 **Important**: The Eclipse Daanse Probe server provides **live catalog monitoring** and automatic reloading capabilities:

#### Dynamic Catalog Updates
- **File Monitoring**: The server continuously watches the `/app/catalog/` directory and all subdirectories
- **Automatic Reloading**: Any changes to catalog files trigger automatic reloading into the internal database
- **Hot Deployment**: No container restart required for catalog updates

#### Supported Runtime Changes
- **XMI Mapping Files**: Modify `catalog.xmi` files to update cube definitions, measures, dimensions
- **CSV Data Files**: Add, modify, or delete CSV files to update table data
- **New Catalogs**: Add complete new catalog directories
- **Schema Changes**: Create/modify schema folders and their contents
- **File Deletions**: Remove catalogs or data files


#### Development Workflow
1. **Mount catalog directory** with write access (remove `:ro` flag for development):
   ```bash
   # Docker/Podman - Development mode
   podman run -v ~/temp/probe/catalog:/app/catalog/:Z -v ~/temp/probe/output:/app/output/:Z -p 8095:8080 eclipsedaanse/probe:snapshot
   ```

2. **Edit catalogs** using any text editor or IDE

3. **Observe automatic reloading** in the container logs:
   ```bash
   # Docker/Podman - Watch logs
   podman logs -f probe
   docker logs -f probe
   # Watch for catalog reload messages
   ```

4. **Verify Documentation** - verify the docs in `output/documentation` folder

5. **Test changes immediately** - no restart needed, connect via odc file in `output/odc`


### Best Practices

- **Backup Catalogs**: Keep backup copies before making changes
- **Change Management**: Use version control for catalog definitions
- **Performance Impact**: Large catalog changes may cause temporary performance impact during reload
- **Consistent naming**: Use descriptive catalog and table names
- **Schema organization**: Group related tables in schemas
- **Data types**: Match XMI column types with CSV type definitions
- **File encoding**: Use UTF-8 encoding for international characters
- **Performance**: Keep fact tables reasonably sized for tutorials
- **Development**: Use writable mounts for development, read-only for production
- **Monitoring**: Watch container logs for catalog reload confirmations

## Usage

### XMLA Endpoint

The Probe server exposes its XMLA (XML for Analysis) endpoint at:

**Endpoint URL**: `http://localhost:8095/xmla`

#### Connecting from Excel
1. **Excel 2016+**: 
   - Data → Get Data → From Other Sources → From Analysis Services
   - Server: `localhost:8095/xmla`
   - Connection type: HTTP
   - **Username**: Any username (e.g., `demo`, `testuser`, or `admin|role1|role2`)
   - **Password**: Leave empty (no password required)

2. **Excel with OLEDB**:
   - Data → Get Data → From Other Sources → From OLEDB  
   - Connection string: `Provider=MSOLAP;Data Source=http://localhost:8095/xmla;User ID=demo`
   - **Password**: Leave empty

3. **Power BI Desktop**:
   - Get Data → Analysis Services → Connect live
   - Server: `localhost:8095/xmla`
   - **Username**: Any username
   - **Password**: Leave empty

#### Connecting from Other BI Tools
- **Tableau**: Use "Microsoft Analysis Services" connector with `localhost:8095/xmla`
- **Power BI Service**: Configure on-premises data gateway pointing to the XMLA endpoint
- **Custom Applications**: Use any XMLA/OLEDB client library

### Authentication & Role Testing

Eclipse Daanse Probe uses a **simplified authentication system** designed for development and testing - nothing is verified:

#### Anonymous Requests
Requests without credentials are served as the stand-in identity `probe`
carrying the role **Administrator**. Roles are intersected with the roles a
catalog declares, so this only opens catalogs that actually declare an
`Administrator` role (the shipped FoodMart tutorial does). Set
`-Ddaanse.probe.requireLogin=true` to challenge instead.

#### Basic Authentication
- **Username**: Any username (no validation), optionally with roles appended (see below)
- **Password**: **Always leave empty** (credentials are acknowledged unverified, realm `Daanse Probe`)
- **Purpose**: Development-friendly authentication for quick testing

#### Role-Based Access Control Testing

For testing catalog security and access control, you can specify **roles directly in the username**:

**Username Format**: `username|role1|role2|role3|...`

The role names must be roles the catalog declares - roles are intersected
with the catalog's own, and an unknown name leaves the caller with none
(FoodMart declares `Administrator`, `California manager` and `No HR Cube`).

#### Examples

1. **Basic User**:
   ```text
   Username: demo
   Password: (empty)
   Result: Default access rights
   ```

2. **Single Role Testing**:
   ```text
   Username: testuser|manager
   Password: (empty)  
   Result: Access with "manager" role permissions
   ```

3. **Multiple Roles Testing**:
   ```text
   Username: admin|sales|finance|reporting
   Password: (empty)
   Result: Combined permissions from all specified roles
   ```


#### Role Testing Use Cases

- **🔐 Access Control Validation**: Verify that cube and dimension security rules work correctly
- **👥 Multi-Role Scenarios**: Test users with multiple organizational roles  
- **🧪 Permission Testing**: Validate that restricted data is properly filtered
- **📊 Catalog Security**: Ensure tutorial catalogs demonstrate proper security patterns


## Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Use different host port (Docker/Podman)
   docker run -p 8096:8080 eclipsedaanse/probe:snapshot
   podman run -p 8096:8080 eclipsedaanse/probe:snapshot
   ```

2. **Permission Issues with Volumes**
   ```bash
   # Ensure correct ownership
   sudo chown -R 1000:1000 /path/to/catalogs
   ```

3. **Memory Issues**
   ```bash
   # Increase memory limit (Docker/Podman)
   docker run --memory=4g eclipsedaanse/probe:snapshot
   podman run --memory=4g eclipsedaanse/probe:snapshot
   ```


## Requirements
- Java 25+ JRE or JDK (the start script refuses older versions - the bundles declare osgi.ee=JavaSE-25)
- 512MB+ RAM (recommended: 1GB+)
- Write permissions for log directory
- Network access for the XMLA endpoint (container port 8080)

## Security Considerations

- Container runs as non-root user (UID 1000)
- Use read-only volume mounts for catalog data
- Implement network policies in production
- Regular security updates for base images

## Support

For issues and feature requests:
- Eclipse Daanse Project: [Eclipse Foundation](https://www.eclipse.org/daanse/)
- GitHub Issues: Report bugs and feature requests
- Documentation: Check the project's README files and [daanse.org](https://daanse.org)

## License

Eclipse Public License 2.0 (EPL-2.0)

Copyright (c) 2025 Contributors to the Eclipse Foundation
