# COPY-ON-WRITE

## waht is copy-on-write?
Copy-on-write (COW) is an optimization strategy used in computer programming and operating systems to efficiently manage memory. It allows multiple processes to share the same memory pages until one of them attempts to modify a page. At that point, a copy of the page is made for the modifying process, ensuring that changes do not affect other processes sharing the original page.

## How does copy-on-write work?
1. **Shared Memory**: Initially, multiple processes can share the same memory pages. This is efficient because it reduces memory usage and allows for faster access to shared data.
2. **Modification Trigger**: When a process attempts to write to a shared page, the operating system intercepts this action.
3. **Page Copying**: The operating system creates a copy of the page in memory. The original page remains unchanged, allowing other processes to continue using it.
4. **Update Pointer**: The modifying process's page table is updated to point to the new copy of the page, while other processes continue to reference the original page.
5. **Independent Changes**: The modifying process can now make changes to its copy of the page without affecting the original page or other processes.

