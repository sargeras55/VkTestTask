package com.vk.task.presentation.screens.game

import com.vk.core.presentation.controller.DispatcherStateful
import com.vk.core.presentation.controller.StateController
import com.vk.core.presentation.controller.StateHolder
import com.vk.core.presentation.controller.ViewModelStateful
import com.vk.core.presentation.state.ViewProperty
import com.vk.core.presentation.state.ViewState
import com.vk.core.utils.extensions.accept
import com.vk.core.utils.extensions.optional
import com.vk.task.data.game.Game
import com.vk.task.data.game.GameAnswer
import com.vk.task.data.game.GameResult
import com.vk.task.domain.card.GameStore
import com.vk.task.presentation.screens.result.ResultScreen
import com.vk.task.utils.DirectionType
import com.vk.task.utils.EMPTY_STR
import io.reactivex.disposables.CompositeDisposable
import ru.terrakok.cicerone.Router


// ViewState
data class GameViewState(
    val game: ViewProperty<Game>,
    val isLoading: ViewProperty<Boolean>
) : ViewState


// StateController
class GameStateController : StateController<GameViewState> {

    private val dirtyState = GameDirtyState(EMPTY_STR)
    override val state: GameViewState = createState()

    override fun createState(): GameViewState {
        return GameViewState(
            game = ViewProperty.create(null),
            isLoading = ViewProperty.create(true)
        )
    }

    private data class GameDirtyState(
        val countOf: String
    )
}

// Dispatcher
interface GameDispatcher : DispatcherStateful<GameViewState> {
    fun swipedNext(direction: DirectionType)
}


// ViewModel
class GameViewModel(
    private val router: Router,
    private val cardStore: GameStore
) : ViewModelStateful<GameViewState>(), GameDispatcher, StateController<GameViewState> {

    override val disposables: CompositeDisposable = CompositeDisposable()
    override val state: GameViewState = createState()

    private var answers = mutableListOf<GameAnswer>()
    private var currentPosition = 0

    init {
        cardStore.createNewGame()
            .bindSubscribe(
                onSuccess = { newGame ->
                    currentPosition = 0

                    state accept {
                        game.update(newGame)
                        isLoading.update(false)
                    }

                })
    }

    override fun getStateHolder(): StateHolder<GameViewState> {
        return this
    }

    override fun createState(): GameViewState {
        return GameViewState(
            game = ViewProperty.create(null),
            isLoading = ViewProperty.create(true)
        )
    }

    override fun swipedNext(direction: DirectionType) {
        val position = currentPosition

        state.game.currentValue() optional { game ->
            val card = game.cards[position]

            val answerShow = when (direction == DirectionType.LEFT) {
                true -> game.leftShow
                else -> game.rightShow
            }

            val isRightAnswer = answerShow.id == card.show.id
            answers.add(GameAnswer(
                card.character,
                isRightAnswer,
                answerShow,
                card.show
            ))

            currentPosition++

            if (currentPosition == game.cards.size) {
                answers.count()
                val result = GameResult(
                    title = game.title,
                    answers = answers.reversed(),
                    totalPoints = answers.size,
                    earnedPoints = answers.count(GameAnswer::isRightAnswer)
                )
                cardStore.storeResult(result)
                    .bindSubscribe(onSuccess = {
                        router.navigateTo(ResultScreen())
                    })
            }
        }
    }
}
