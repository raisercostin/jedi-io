# input/output locations #

The main purpose of this project is to provide uniform, fluent access to various input and output data locations.

- Reading from a file:
 ```
	Locations.file("/home/costin/myfile.txt").readContent
 ```

 - Copying a file to a new folder (and create 
 ```
	Locations.file("/home/costin/myfile.txt").copyTo(Locations.file("/home/costin/folder2/myfile2.txt").mkdirOnParentIfNecessary))
 ```
