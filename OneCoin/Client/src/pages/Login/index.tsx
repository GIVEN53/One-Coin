import Layout from 'components/Layout';
import React from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
	LoginBox,
	Form,
	Input,
	Errormsg,
	SubmitButton,
	InputContainer,
	StyledDiv,
	MentDiv,
	MentSpan,
	KakaoButton,
} from './style';

type EnterForm = {
	email: string;
	password: string;
};

function Login() {
	const navigate = useNavigate();
	const {
		register,
		handleSubmit,
		watch,
		formState: { errors },
	} = useForm<EnterForm>({
		mode: 'onChange',
	});
	console.log(watch());
	const onLogin = async (data: EnterForm) => {
		console.log();

		try {
			const res = await axios.post(
				'http://13.209.194.104:8080/api/auth/login',
				{
					email: data.email,
					password: data.password,
				}
			);
			if (res.status === 200) {
				if (res.headers.authorization && res.headers.refresh) {
					sessionStorage.setItem('login-token', res.headers.authorization);
					sessionStorage.setItem('login-refresh', res.headers.refresh);
					navigate('/');
				}
			}
		} catch (err) {
			// setError(err.response.data.message);
			setTimeout(() => {
				// setError('');
			}, 2000);
		}
	};

	return (
		<Layout isLeftSidebar={false}>
			<LoginBox>
				<Form onSubmit={handleSubmit(onLogin)}>
					<StyledDiv>로그인</StyledDiv>
					<InputContainer>
						<Input
							placeholder="이메일을 입력해주세요"
							type={'email'}
							id="email"
							{...register('email', {
								required: true,
								pattern: /^\S+@\S+$/i,
								maxLength: 50,
							})}
						/>
						{errors.email && errors.email.type === 'required' && (
							<Errormsg>⚠ 이메일을 입력해주세요.</Errormsg>
						)}
						{errors.email && errors.email.type === 'pattern' && (
							<Errormsg>⚠ 이메일 형식이여야 합니다.</Errormsg>
						)}
						{errors.email && errors.email.type === 'maxLength' && (
							<Errormsg>⚠ 최대 길이는 50자 이하여야 합니다</Errormsg>
						)}
					</InputContainer>
					<InputContainer>
						<Input
							placeholder="비밀번호를 입력해주세요"
							type={'password'}
							id="password"
							{...register('password', {
								required: true,
								minLength: 8,
								pattern: /^(?=.*[a-zA-Z])(?=.*[!@#$%^*+=-])(?=.*[0-9]).{8,25}$/,
							})}
						/>
						{errors.password && errors.password.type === 'required' && (
							<Errormsg>⚠ 비밀번호를 입력해주세요</Errormsg>
						)}
						{errors.password && errors.password.type === 'minLength' && (
							<Errormsg>⚠ 최소 길이는 8자 이상이여야 합니다</Errormsg>
						)}
						{errors.password && errors.password.type === 'pattern' && (
							<Errormsg>
								⚠ 숫자+영문자+특수문자 8자리 이상이여야 합니다
							</Errormsg>
						)}
					</InputContainer>
					<SubmitButton type="submit">로그인</SubmitButton>
					{/* {error && <Errormsg>⚠ {error}</Errormsg>} */}
					<MentDiv>
						<MentSpan onClick={() => navigate('/signup')}>회원가입</MentSpan>
						<MentSpan onClick={() => navigate('/findpassword')}>
							비밀번호 찾기
						</MentSpan>
					</MentDiv>
				</Form>
				<InputContainer>
					<KakaoButton>
						<img
							src="//k.kakaocdn.net/14/dn/btqCn0WEmI3/nijroPfbpCa4at5EIsjyf0/o.jpg"
							width="242"
						/>
					</KakaoButton>
				</InputContainer>
			</LoginBox>
		</Layout>
	);
}

export default Login;