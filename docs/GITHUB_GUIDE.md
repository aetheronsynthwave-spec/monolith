# Putting your project on GitHub – step by step, no experience needed

Think of GitHub as a **safe-deposit box on the internet for your project folder**. Every time you "save a snapshot" it remembers the old version too,
so you can never lose work. You will use a free program with buttons, **GitHub Desktop**. No typing of commands.

## Part 1 – One-time setup (about 15 minutes)
1. **Make an account.** Go to **github.com**, press **Sign up**. Use your email, pick a username and a password you will remember, and write them down.
   GitHub emails you a code; type it in.
2. **Install GitHub Desktop.** Go to **desktop.github.com**, press the big purple **Download** button, open the file, let it install.
3. **Sign in.** Open GitHub Desktop, press **Sign in to GitHub.com**, a web page opens, press **Authorize**, and come back to the program.
4. When it asks for your name and email, type them and press **Continue**.

## Part 2 – Find the right folder
You want the folder that holds your **whole project** (the one with `build.gradle` and a `src` folder inside), not just the `.jar` file.
Put the files from this update into it first (see INSTALL.md).

## Part 3 – Put it online
1. In GitHub Desktop choose **File → Add local repository…**
2. Press **Choose…**, pick your project folder, press **Add repository**.
   * If it says "this directory does not appear to be a Git repository", press the blue words **create a repository**, then **Create repository**.
3. Look at the left side: it lists every changed file with a tick box. Leave them all ticked.
4. At the bottom left, in the box that says **Summary**, type a short note, for example: `ESP boxes and zoom fix`.
5. Press the blue **Commit to main** button. (A "commit" is a saved snapshot.)
6. At the top press **Publish repository**.
7. A small window opens:
   * **Name:** for example `monolith-client`.
   * **Keep this code private** – tick it if you want only yourself to see it (recommended). Untick it to share publicly.
8. Press **Publish repository**. Done! Go to **github.com**, press your picture (top right) → **Your repositories**: your project is there.

## Part 4 – Every time you change something afterwards
1. Open GitHub Desktop (it finds your project by itself).
2. Type a short note in **Summary**.
3. Press **Commit to main**.
4. Press **Push origin** at the top. That sends the snapshot online.

## Part 5 – Getting an old version back
Click **History** (top left, next to *Changes*). Right-click any older snapshot → **Revert changes in commit**.

## Do's and don'ts
* ✅ Upload the **source code** (`src` folder, `build.gradle`, `gradle` files). Compiled `.jar` files go in GitHub's **Releases** section instead (on the repository page, press **Releases → Create a new release → drag the jar in**).
* ❌ **Never upload passwords, account tokens, Minecraft login files, or your `options.txt`/`config` folders.** The included `.gitignore` already skips build junk; if a secret ever goes online, change the password immediately.
* A **.gitignore** file tells GitHub which things to skip. Copy the one in this folder into your project's main folder before step 3 of Part 3.
* If you make the project **public**, choose a licence: on the repository page press **Add file → Create new file**, name it `LICENSE`, press **Choose a license template**, pick **MIT** (simple and permissive).
* Stuck? Take a screenshot and send it to me; I will tell you exactly which button to press.
