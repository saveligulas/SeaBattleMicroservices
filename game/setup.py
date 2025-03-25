import os
import subprocess
import sys

def clone_git_project():
    repo_url = "https://github.com/saveligulas/sg-spring-extras"

    current_dir = os.path.dirname(os.path.abspath(__file__))

    repo_name = "sg-spring-extras"
    target_dir = os.path.join(os.path.dirname(current_dir), repo_name)

    if os.path.exists(target_dir):
        print(f"Repository '{repo_name}' already exists at {target_dir}")
        return

    try:
        subprocess.run(["git", "clone", repo_url, target_dir], check=True)
        print(f"Cloned '{repo_name}' into {target_dir}")
    except subprocess.CalledProcessError as e:
        print(f"Failed to clone repository: {e}")
        sys.exit(1)

if __name__ == "__main__":
    clone_git_project()