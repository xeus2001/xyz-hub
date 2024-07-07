# README
This folder contains EC2 instance configurations needed to run Naksha-Hub or the PostgresQL database docker that is used by Naksha-Hub on EC2 instances.

## Install
To install a new EC2 Postgres instance the following steps are needed:

- Create all resources with tag `odin_add_id` and `Name`.
- Create a placement group with strategy `cluster`, name `naksha_db_cluster`
- Create two new Network Interfaces with n being `1` and `2`:
  - Description: `Naksha PostgresQL ENI-{n}`
  - Add into subnet `subnet-0a600ceb17a8e19fb` (_public E2E us-east-1a_0_).
  - Private IPv4 address: Auto-assign
  - Add security group `sg-0a39beed168bd9e97` (_wikvaya-e2e-us-east-1-sg_).
  - odin_app_id: `661`
  - Name: `naksha_db_eni_{n}`
- Start a new EC2 instance of the type `r6idn.metal` in the new placement group.
  - Use **256 GiB** of **gp3** for root storage.
  - Locate the instance in **us-east-1** region.
  - Locate in `vpc-0c0b607d333227c5e` (_Direct Connect VPC E2E_).
  - Add into subnet `subnet-0a600ceb17a8e19fb` (_public E2E us-east-1a_0_).
  - Assign a public IP, so that you get access to docker registry.
  - Add security group `sg-0a39beed168bd9e97` (_wikvaya-e2e-us-east-1-sg_).
    - This allows all incoming traffic from `10.0.0.0/8`.
  - Attach the two network interfaces (they will not have a public IP).
- Then create 16 EBS volumes using **gp3** type
  - Size: **1024 GiB**
  - Throughput: **1000 MiB/s**
  - IOPS: **16000**
  - Name: `naksha_postgresql_perftest_vNN` (with NN being **00** to **15**) 
  - Attach them to the instance.
- Create Route 53 records
  - Grab the IPs associated with the two ENIs (`ipconfig`, `ens2` and `ens3`)
  - Create two DNS records with the same name (`naksha-db.e2e` `.cmtrd.aws.in.here.com`)
  - TTL: 1m
  - Routing policy: Weighted
  - Weight: 100
  - Heath-check ID: -
  - Record IDs: `naksha_db_e2e_eni_1` and `naksha_db_e2e_eni_2`
 
Ones done, open up a shell to the machine and start the installation:

```bash
# Compact all files and upload them to the new instance
tar czf files.tgz files
scp files.tgz ec2-user@<ip>:.

# Enter the machine
ssh ec2-user@IP

# Install necessary software
sudo yum -y install docker mdadm postgresql15 nc nmap fio nvme-cli mlocate

# Unpack the files and install them
tar xzf files.tgz
sudo chown -R root:root files
sudo cp files/etc/security/limits.conf /etc/security/limits.conf
sudo cp files/etc/systemd/networkd.conf /etc/systemd/networkd.conf
sudo cp files/etc/sysctl.conf /etc/sysctl.d/99-sysctl.conf

# Reboot the machine (Note: This can easily require 15 minutes, before you can log in again!!!)
sudo shutdown -r 0

# Login back to the machine, check if new settings are accepted
sudo cat /proc/sys/net/ipv4/tcp_available_congestion_control    # should show cubic
sudo cat /proc/sys/net/ipv4/tcp_max_tw_buckets                  # should show 262144
sudo cat /proc/sys/net/ipv4/tcp_mem                             # should show 2097152	4194304	6291456

# Ensure that names are still what this document state.
lsblk

# Ensure that block-size is 4k
nvme id-ns -H /dev/nvme0n1 | grep LBA

# If not using 4k blocks format, change it (optimal would be 32K)
# Note, by default devices always use 512b formats!
nvme format --lbaf=1 /dev/nvme0n1

# Create temporary store
sudo mdadm --create --verbose --chunk=32 /dev/md0 --level=0 --name=pg_temp --raid-devices=4 /dev/nvme0n1 /dev/nvme1n1 /dev/nvme2n1 /dev/nvme3n1

# Create consistent store
sudo mdadm --create --verbose --chunk=32 /dev/md1 --level=0 --name=pg_data --raid-devices=16 /dev/nvme5n1 /dev/nvme6n1 /dev/nvme7n1 /dev/nvme8n1 /dev/nvme9n1 /dev/nvme10n1 /dev/nvme11n1 /dev/nvme12n1 /dev/nvme13n1 /dev/nvme14n1 /dev/nvme15n1 /dev/nvme16n1 /dev/nvme17n1 /dev/nvme18n1 /dev/nvme19n1 /dev/nvme20n1

# Review that the RAID is created
sudo cat /proc/mdstat

# Store the configuration
sudo mdadm --detail --scan --verbose | sudo tee -a /etc/mdadm.conf

# Ensure that the file looks like this:
ARRAY /dev/md0 level=raid0 num-devices=4 metadata=1.2 name=pg_temp UUID=9c1f21ea:eefb8a1c:a7f57b53:8d7e50bb devices=/dev/nvme0n1,/dev/nvme1n1,/dev/nvme2n1,/dev/nvme3n1
ARRAY /dev/md1 level=raid0 num-devices=16 metadata=1.2 name=pg_data UUID=7f469a97:bd0113bf:a2f4e2fe:1dbc9994 devices=/dev/sdb,/dev/sdc,/dev/sdd,/dev/sde,/dev/sdf,/dev/sdg,/dev/sdh,/dev/sdi,/dev/sdj,/dev/sdk,/dev/sdl,/dev/sdm,/dev/sdn,/dev/sdo,/dev/sdp,/dev/sdq
# If it does not, then fix it accordingly using 

# A device can be restored via
sudo mdadm -A /dev/md1

# Create file systems
# -m = reserved space for root (we do not need this)
# -b = block-size, should always be the same as the MMU, so always 4k
# -C = chunk-size, bigalloc amount of byte to allocate in chunk
# -g = blocks-per-group, defaults to 32768 (128 MiB), with this setting all groups start at device 0 in the raid!
# -O Enable additional features, here bigalloc (chunk allocation) and no journal
# -E RAID parameters
#   stride: essentials the chunk-size (aka stride-size) of the raids (we want 32kb)
#   stripe-width: the product of number of disks and stride-size 
# In a nutshell: Either use an odd number of drives with corrsponding un-even stripe-width or change the group size to prevent that all groups start at disk #0!
# see: https://manpages.debian.org/experimental/e2fsprogs/mkfs.ext4.8.en.html
sudo mkfs.ext4 -m 0 -b 4096 -C 32768 -g 32760 -O bigalloc,^has_journal -E stride=32,stripe-width=128 /dev/md0
sudo mkfs.ext4 -m 0 -b 4096 -C 32768 -g 32760 -O bigalloc,^has_journal -E stride=32,stripe-width=512 /dev/md1

# Create postgres user
sudo groupadd postgres -g 5430
sudo useradd postgres -u 5432 -g 5430

# Mount the disks
sudo mkdir -p /mnt/pg_temp && sudo chown postgres:postgres /mnt/pg_temp
sudo mkdir -p /mnt/pg_data && sudo chown postgres:postgres /mnt/pg_data
sudo mount /dev/md0 /mnt/pg_temp 
sudo mount /dev/md1 /mnt/pg_data

# Patch /etc/fstab, so this gets auto-mounted
sudo echo "/dev/md0        /mnt/pg_temp    ext4    defaults,noatime,nofail,discard" | sudo tee -a /etc/fstab
sudo echo "/dev/md1        /mnt/pg_data    ext4    defaults,noatime,nofail,discard" | sudo tee -a /etc/fstab
cat /etc/fstab

# Remove lost and found folders (this prevents initdb)
sudo rm -rf /mnt/pg_temp/lost+found/ /mnt/pg_data/lost+found/

# Redirect port 80 to 5432, so we can connect to PostgresQL from VPN
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 5432

# Ensure docker uses enough virtual memory and files for postgresql
# Note: -i saves the modification back to the file, -r just does replace
sudo sed -i -r 's/OPTIONS="--default-ulimit nofile=32768:65536"/OPTIONS="--default-ulimit nofile=1048576:1048576 --default-shm-size=16384m"/' /etc/sysconfig/docker
sudo cat /etc/sysconfig/docker

# Start docker (and make it auto-starting)
# see: https://docs.docker.com/reference/cli/dockerd/#daemon-configuration-file
sudo systemctl start docker
sudo systemctl enable docker

# Start the PostgresQL docker
sudo docker pull hcr.data.here.com/naksha-devops/naksha-postgres:amd64-v16.2-r0
sudo docker run --name naksha_pg --privileged -v /mnt/pg_data:/usr/local/pgsql/data -v /mnt/pg_temp:/usr/local/pgsql/temp --network host -d hcr.data.here.com/naksha-devops/naksha-postgres:amd64-v16.2-r0
sudo docker logs naksha_pg
# Find the generated postgres root password, looks like:
# Initialized database with password: zFKRsAEWoJteGUCobBxgJmNrDLeJARNP
# Now update configuration
sudo docker stop naksha_pg
sudo vim /mnt/pg_data/postgresql.conf
# Jump to the end of the file, uncomment the line
# include_if_exists = '/home/postgres/r6idn.metal.conf'
# Then save and exit, restart docker
sudo docker start naksha_pg

# When the docker runs, enable auto-restart
sudo docker update --restart unless-stopped naksha_pg
```

## Management

### Docker
```bash
# Stop docker
sudo docker stop naksha_pg
# Remove docker (delete the container)
sudo docker rm naksha_pg
# Restart docker
sudo docker start naksha_pg
```

### Mounts
```bash
sudo umount /mnt/pg_data
sudo umount /mnt/pg_temp
```

### RAID
Requires that docker is shut down.
```bash
# Stop the raid
sudo mdadm --stop /dev/md1
sudo mdadm --stop /dev/md0
# Remove all drives from RAID
sudo mdadm --remove /dev/md{0|1}
# Remove failed drive
sudo mdadm --fail /dev/nvme{n}n1 --remove /dev/nvme{n}n1
# Zero superblock
sudo mdadm --zero-superblock /dev/nvme{n}n1
```

Fore more information see [How to remove software raid with mdadm](https://www.diskinternals.com/raid-recovery/how-to-remove-software-raid-with-mdadm/).

## Debugging
Test the connection:
```bash
psql "user=postgres sslmode=disable host=localhost dbname=unimap"
#or
psql "user=postgres sslmode=disable host=naksha-db.e2e.cmtrd.aws.in.here.com dbname=unimap"
```

Review if the socket is bound:
```bash
sudo netstat -tulpn | grep :5432
sudo nmap localhost
```

Show which sockets are listened to:
```bash
sudo lsof -i -P -n | grep LISTEN
```