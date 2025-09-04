package com.habittracker.nudges.data

/**
 * Repository of motivational quotes for different scenarios
 */
object MotivationalQuotes {
    
    val streakBreakWarnings = listOf(
        "Your ${"%d"}-day streak is at risk! Don't let today be the day you break it.",
        "You've come so far with your ${"%d"}-day streak. One small action can keep it alive!",
        "Your ${"%d"} days of progress deserve one more day. Keep going!",
        "Breaking a ${"%d"}-day streak would be a shame. You've got this!",
        "Your future self will thank you for maintaining this ${"%d"}-day streak today."
    )
    
    val motivationalQuotes = listOf(
        "Small steps every day lead to big changes in a year.",
        "Success is the sum of small efforts repeated day in and day out.",
        "You don't have to be perfect, you just have to be consistent.",
        "Progress, not perfection, is the goal.",
        "Every expert was once a beginner. Every pro was once an amateur.",
        "The best time to plant a tree was 20 years ago. The second best time is now.",
        "Don't watch the clock; do what it does. Keep going.",
        "A journey of a thousand miles begins with a single step.",
        "The only impossible journey is the one you never begin.",
        "Believe you can and you're halfway there.",
        "It's not about perfect days, it's about not giving up.",
        "Your only limit is your mind.",
        "Great things never come from comfort zones.",
        "Dream it. Believe it. Build it.",
        "Consistency beats perfection every time."
    )
    
    val celebrationQuotes = listOf(
        "Congratulations on maintaining your streak! You're building something amazing.",
        "Look at you go! Another day of progress in the books.",
        "Your dedication is paying off. Keep up the fantastic work!",
        "You're proving that consistency creates results. Well done!",
        "Every day you choose progress over perfection. That's inspiring!",
        "Your commitment to this habit is truly admirable.",
        "You're building a better version of yourself, one day at a time.",
        "This is what dedication looks like. You should be proud!",
        "Your consistency is your superpower. Keep wielding it!",
        "You're not just building a habit, you're building character."
    )
    
    val encouragementAfterMiss = listOf(
        "Missing one day doesn't erase your progress. Get back on track today!",
        "Every champion has missed days. What matters is getting back up.",
        "Your journey isn't over because of one missed day. Keep going!",
        "Setbacks are setups for comebacks. Today is your comeback day!",
        "One day doesn't define your journey. Your next action does.",
        "The path to success isn't a straight line. Get back on course!",
        "You're human, and humans aren't perfect. What matters is persistence.",
        "Champions aren't made by never falling, but by always getting back up.",
        "Your previous progress proves you can do this. Start again today!",
        "A fresh start is just one decision away. Make that decision now."
    )
    
    val goalSuggestions = listOf(
        "Consider starting with just 5 minutes a day. Small wins build momentum!",
        "What if you tried this habit every other day to start?",
        "Maybe break this down into smaller, easier steps?",
        "Could you make this habit easier to fit into your routine?",
        "Sometimes less is more. What's the smallest version of this habit?",
        "Consider linking this habit to something you already do daily.",
        "What time of day might work better for this habit?",
        "Could you modify this habit to be more enjoyable?",
        "What obstacles are making this hard? Let's work around them.",
        "Progress over perfection. What's a easier version you'd actually do?"
    )
    
    fun getRandomStreakWarning(streakDays: Int): String {
        return streakBreakWarnings.random().format(streakDays)
    }
    
    fun getRandomMotivationalQuote(): String {
        return motivationalQuotes.random()
    }
    
    fun getRandomCelebrationQuote(): String {
        return celebrationQuotes.random()
    }
    
    fun getRandomEncouragementAfterMiss(): String {
        return encouragementAfterMiss.random()
    }
    
    fun getRandomGoalSuggestion(): String {
        return goalSuggestions.random()
    }
}
