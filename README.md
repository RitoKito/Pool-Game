# Pool Game Builder

To run the application, please use:

gradle run

# Game Notes
- Upon launching the game you will be asked to select one of four difficulties via user interface, after selection the game will launch selected difficulty.
- Difficulty can be changed via "Game Settings" button at the bottom right corner, followed by "Change Difficulty" button.
- You can remove specific coloured balls via accessing "Game Settings", followed by "Cheat: Remove Balls" button and selecting colour of balls to be removed.
- You can undo last action whether it was a cheat or a hit via "Undo Last Action" button. Difficulty change cannot be undo, moreover, when difficulty is changed
recorded actions are lost.
- In order to hit the ball, click and hold onto the edge of the cue ball where you'd like to hit. 
- Then, drag your cursor away (in the angle you'd like to hit), and then release.
- The power of your hit will be based on the length of your drag (although ball velocity is capped).

- You can modify game rules via \src\main\resources\ .json files
- Currently the game features 4 difficulty levels
