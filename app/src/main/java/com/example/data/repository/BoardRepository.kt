package com.example.data.repository

import com.example.data.local.BoardDao
import com.example.data.local.BoardEntity
import kotlinx.coroutines.flow.Flow

class BoardRepository(private val boardDao: BoardDao) {
    val allBoards: Flow<List<BoardEntity>> = boardDao.getAllBoards()

    suspend fun getBoardById(id: Long): BoardEntity? = boardDao.getBoardById(id)

    suspend fun saveBoard(board: BoardEntity): Long {
        return if (board.id == 0L) {
            boardDao.insertBoard(board)
        } else {
            boardDao.updateBoard(board)
            board.id
        }
    }

    suspend fun deleteBoard(id: Long) = boardDao.deleteBoardById(id)
}
