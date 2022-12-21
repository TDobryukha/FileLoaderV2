# FileLoaderV2
Downloads files over HTTP in multiple streams with a download speed limit. Implementation No. 2.
For thread management, ExecutorService and CompletableFuture are used. To limit the download speed RateLimiter is used.
User needs to specify:
1.Full path to the text file, which contains a list of links of what needs to be downloaded. (One link per line)
2. Full path to the folder where you want to upload files
3. Number of threads for downloading files simultaneously
4. Limit on download speed (in KB). If set 0, there will be no download speed limit.
